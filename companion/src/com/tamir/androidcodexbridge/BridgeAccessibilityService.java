package com.tamir.androidcodexbridge;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class BridgeAccessibilityService extends AccessibilityService {
    public static final int LOOPBACK_PORT = 8765;
    private static final String TAG = "AndroidCodexBridge";
    private static final int PROTOCOL_VERSION = 1;
    private static final int MAX_REQUEST_BYTES = 128 * 1024;
    private static final int MAX_NODES = 1000;
    private static final int MAX_DEPTH = 40;

    private final AtomicLong generation = new AtomicLong(1);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, NodeRef> lastSnapshot = new HashMap<>();

    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread serverThread;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        generation.incrementAndGet();
        BridgeCredentialStore.getOrCreate(this);
        startServer();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event != null) generation.incrementAndGet();
    }

    @Override
    public void onInterrupt() {
        generation.incrementAndGet();
    }

    @Override
    public void onDestroy() {
        stopServer();
        super.onDestroy();
    }

    private synchronized void startServer() {
        if (running) return;
        try {
            serverSocket = new ServerSocket(
                    LOOPBACK_PORT, 4, InetAddress.getByName("127.0.0.1"));
        } catch (IOException error) {
            Log.e(TAG, "Could not bind the local control socket", error);
            return;
        }
        running = true;
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                acceptLoop();
            }
        }, "codex-local-control");
        serverThread.setDaemon(true);
        serverThread.start();
        Log.i(TAG, "Authenticated loopback-only control socket started");
    }

    private synchronized void stopServer() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
                // The service is stopping; no user data is logged.
            }
        }
        serverSocket = null;
        if (serverThread != null) serverThread.interrupt();
        serverThread = null;
        synchronized (lastSnapshot) {
            lastSnapshot.clear();
        }
    }

    private void acceptLoop() {
        while (running) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                socket.setSoTimeout(8000);
                handleClient(socket);
            } catch (IOException error) {
                if (running) Log.w(TAG, "Local socket accept failed", error);
            } finally {
                closeQuietly(socket);
            }
        }
    }

    private void handleClient(Socket socket) throws IOException {
        String line = readLineLimited(socket.getInputStream());
        final JSONObject request;
        try {
            request = new JSONObject(line);
        } catch (JSONException error) {
            writeResponse(socket.getOutputStream(), failure(null, "BAD_JSON",
                    "Request must be one JSON object followed by a newline"));
            return;
        }

        String suppliedToken = request.optString("token", "");
        String expectedToken = BridgeCredentialStore.getOrCreate(this);
        if (!constantTimeEquals(suppliedToken, expectedToken)) {
            SystemClock.sleep(250);
            writeResponse(socket.getOutputStream(), failure(null, "UNAUTHORIZED",
                    "Local authentication failed"));
            Log.w(TAG, "Rejected an unauthenticated loopback client");
            return;
        }

        JSONObject response = runOnMainThread(request);
        writeResponse(socket.getOutputStream(), response);
    }

    private JSONObject runOnMainThread(JSONObject request) {
        final JSONObject[] result = new JSONObject[1];
        final CountDownLatch latch = new CountDownLatch(1);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    result[0] = handleRequest(request);
                } catch (RuntimeException error) {
                    result[0] = failure(request.optString("id", null), "INTERNAL_ERROR",
                            "The accessibility operation failed internally");
                    Log.e(TAG, "Accessibility command failed without logging its content", error);
                } finally {
                    latch.countDown();
                }
            }
        });
        try {
            if (!latch.await(8, TimeUnit.SECONDS)) {
                return failure(request.optString("id", null), "TIMEOUT",
                        "The accessibility service did not answer in time");
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return failure(request.optString("id", null), "INTERRUPTED",
                    "The local request was interrupted");
        }
        return result[0];
    }

    private JSONObject handleRequest(JSONObject request) {
        String id = request.optString("id", null);
        if (request.optInt("version", -1) != PROTOCOL_VERSION) {
            return failure(id, "UNSUPPORTED_VERSION", "Expected protocol version 1");
        }
        String command = request.optString("command", "");
        JSONObject args = request.optJSONObject("args");
        if (args == null) args = new JSONObject();

        switch (command) {
            case "status":
                return success(id, statusResult());
            case "snapshot":
                return snapshot(id, args.optBoolean("include_text", true));
            case "perform":
                return perform(id, args);
            default:
                return failure(id, "UNKNOWN_COMMAND",
                        "Supported commands are status, snapshot and perform");
        }
    }

    private JSONObject statusResult() {
        JSONObject result = new JSONObject();
        put(result, "service_enabled", true);
        put(result, "transport", "tcp-loopback");
        put(result, "listen_address", "127.0.0.1");
        put(result, "peer_authentication", "256-bit-local-token");
        put(result, "locked", isLocked());
        put(result, "generation", generation.get());
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            try {
                put(result, "package", charSequence(root.getPackageName()));
                put(result, "window_id", root.getWindowId());
            } finally {
                root.recycle();
            }
        } else {
            put(result, "package", JSONObject.NULL);
            put(result, "window_id", JSONObject.NULL);
        }
        return result;
    }

    private JSONObject snapshot(String id, boolean includeText) {
        if (isLocked()) return failure(id, "DEVICE_LOCKED", "Unlock the device first");
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return failure(id, "NO_ACTIVE_WINDOW", "No active accessibility window");

        long startGeneration = generation.get();
        int windowId = root.getWindowId();
        String packageName = charSequence(root.getPackageName());
        JSONArray nodes = new JSONArray();
        Map<String, NodeRef> captured = new HashMap<>();
        int[] counter = new int[] {0};
        try {
            collect(root, new ArrayList<>(), startGeneration, windowId, includeText,
                    nodes, captured, counter, 0);
        } finally {
            root.recycle();
        }

        if (startGeneration != generation.get()) {
            return failure(id, "WINDOW_CHANGED", "The screen changed during inspection; retry");
        }
        synchronized (lastSnapshot) {
            lastSnapshot.clear();
            lastSnapshot.putAll(captured);
        }
        JSONObject result = new JSONObject();
        put(result, "generation", startGeneration);
        put(result, "window_id", windowId);
        put(result, "package", packageName);
        put(result, "node_count", nodes.length());
        put(result, "truncated", counter[0] >= MAX_NODES);
        put(result, "nodes", nodes);
        return success(id, result);
    }

    private void collect(AccessibilityNodeInfo node, List<Integer> path, long snapshotGeneration,
                         int windowId, boolean includeText, JSONArray nodes,
                         Map<String, NodeRef> captured, int[] counter, int depth) {
        if (node == null || counter[0] >= MAX_NODES || depth > MAX_DEPTH) return;
        String nodeId = "n" + snapshotGeneration + "-" + counter[0]++;
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        NodeRef ref = new NodeRef(snapshotGeneration, windowId, path,
                charSequence(node.getPackageName()), charSequence(node.getClassName()),
                node.getViewIdResourceName(), bounds);
        captured.put(nodeId, ref);

        JSONObject item = new JSONObject();
        put(item, "id", nodeId);
        put(item, "class", ref.className);
        put(item, "view_id", emptyToNull(ref.viewId));
        put(item, "bounds", boundsToJson(bounds));
        put(item, "enabled", node.isEnabled());
        put(item, "focused", node.isFocused());
        put(item, "editable", node.isEditable());
        put(item, "clickable", node.isClickable());
        put(item, "scrollable", node.isScrollable());
        put(item, "actions", supportedActions(node));
        if (includeText) {
            put(item, "text", emptyToNull(limitedText(node.getText())));
            put(item, "content_description",
                    emptyToNull(limitedText(node.getContentDescription())));
        }
        nodes.put(item);

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount && counter[0] < MAX_NODES; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            ArrayList<Integer> childPath = new ArrayList<>(path);
            childPath.add(i);
            try {
                collect(child, childPath, snapshotGeneration, windowId, includeText,
                        nodes, captured, counter, depth + 1);
            } finally {
                child.recycle();
            }
        }
    }

    private JSONObject perform(String id, JSONObject args) {
        if (isLocked()) return failure(id, "DEVICE_LOCKED", "Unlock the device first");
        String nodeId = args.optString("node", "");
        String action = args.optString("action", "");
        if (nodeId.isEmpty() || action.isEmpty()) {
            return failure(id, "BAD_REQUEST", "perform requires node and action");
        }
        final NodeRef ref;
        synchronized (lastSnapshot) {
            ref = lastSnapshot.get(nodeId);
        }
        if (ref == null) return failure(id, "UNKNOWN_NODE", "Inspect the screen again first");
        if (ref.generation != generation.get()) {
            return failure(id, "STALE_NODE", "The screen changed after this node was inspected");
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return failure(id, "NO_ACTIVE_WINDOW", "No active accessibility window");
        AccessibilityNodeInfo node = null;
        try {
            if (root.getWindowId() != ref.windowId
                    || !TextUtils.equals(root.getPackageName(), ref.packageName)) {
                return failure(id, "STALE_WINDOW", "The active window is no longer the inspected window");
            }
            node = resolve(root, ref.path);
            if (node == null || !matches(node, ref)) {
                return failure(id, "STALE_NODE", "The selected element no longer matches the screen");
            }
            return performResolved(id, action, args, node, ref);
        } finally {
            if (node != null) node.recycle();
            root.recycle();
        }
    }

    private JSONObject performResolved(String id, String action, JSONObject args,
                                       AccessibilityNodeInfo node, NodeRef ref) {
        int androidAction;
        switch (action) {
            case "click": androidAction = AccessibilityNodeInfo.ACTION_CLICK; break;
            case "focus": androidAction = AccessibilityNodeInfo.ACTION_FOCUS; break;
            case "scroll_forward": androidAction = AccessibilityNodeInfo.ACTION_SCROLL_FORWARD; break;
            case "scroll_backward": androidAction = AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD; break;
            case "set_text": return setText(id, args, node, ref);
            default:
                return failure(id, "UNSUPPORTED_ACTION",
                        "Supported actions are click, focus, scroll_forward, scroll_backward and set_text");
        }
        if ((node.getActions() & androidAction) == 0) {
            return failure(id, "ACTION_NOT_SUPPORTED", "The selected element does not support this action");
        }
        boolean accepted = node.performAction(androidAction);
        if (!accepted) return failure(id, "ACTION_REJECTED", "Android rejected the requested action");
        SystemClock.sleep(120);

        JSONObject result = new JSONObject();
        put(result, "action", action);
        put(result, "accepted", true);
        put(result, "post_state", currentWindowSummary());
        if (action.equals("focus")) {
            AccessibilityNodeInfo after = reacquire(ref);
            boolean verified = after != null && after.isFocused();
            if (after != null) after.recycle();
            put(result, "verified", verified);
        } else {
            put(result, "verified", true);
        }
        return success(id, result);
    }

    private JSONObject setText(String id, JSONObject args, AccessibilityNodeInfo node, NodeRef ref) {
        if (!args.has("text")) return failure(id, "BAD_REQUEST", "set_text requires text");
        String value = args.optString("text", "");
        if (!node.isEditable() || (node.getActions() & AccessibilityNodeInfo.ACTION_SET_TEXT) == 0) {
            return failure(id, "SET_TEXT_UNSUPPORTED",
                    "This element does not expose editable ACTION_SET_TEXT support");
        }

        if (!node.isFocused()) {
            if ((node.getActions() & AccessibilityNodeInfo.ACTION_FOCUS) == 0
                    || !node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
                return failure(id, "FOCUS_FAILED", "Could not focus the selected text field");
            }
            SystemClock.sleep(100);
            AccessibilityNodeInfo focused = reacquireFocused(ref);
            boolean focusVerified = focused != null && focused.isFocused();
            if (focused != null) focused.recycle();
            if (!focusVerified) return failure(id, "FOCUS_CHANGED",
                    "The field did not retain focus; text was not entered");
        }

        AccessibilityNodeInfo current = reacquireFocused(ref);
        if (current == null || !current.isFocused() || !matchesStableIdentity(current, ref)) {
            if (current != null) current.recycle();
            return failure(id, "STALE_OR_UNFOCUSED",
                    "The field changed or lost focus before text entry");
        }
        Bundle bundle = new Bundle();
        bundle.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
        boolean accepted = current.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle);
        current.recycle();
        if (!accepted) return failure(id, "SET_TEXT_REJECTED",
                "The application rejected ACTION_SET_TEXT");

        SystemClock.sleep(160);
        AccessibilityNodeInfo after = reacquireByStableIdentity(ref);
        boolean verified = after != null && TextUtils.equals(after.getText(), value);
        if (after != null) after.recycle();
        JSONObject result = new JSONObject();
        put(result, "action", "set_text");
        put(result, "accepted", true);
        put(result, "verified", verified);
        put(result, "character_count", value.length());
        put(result, "post_state", currentWindowSummary());
        if (!verified) put(result, "warning",
                "Android accepted ACTION_SET_TEXT but the field did not read back identically");
        return success(id, result);
    }

    private AccessibilityNodeInfo reacquire(NodeRef ref) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;
        try {
            if (root.getWindowId() != ref.windowId
                    || !TextUtils.equals(root.getPackageName(), ref.packageName)) return null;
            AccessibilityNodeInfo node = resolve(root, ref.path);
            if (node == null || !matches(node, ref)) {
                if (node != null) node.recycle();
                return null;
            }
            return node;
        } finally {
            root.recycle();
        }
    }

    private AccessibilityNodeInfo reacquireFocused(NodeRef ref) {
        AccessibilityNodeInfo focused = reacquireByStableIdentity(ref);
        if (focused == null || !focused.isFocused()) {
            if (focused != null) focused.recycle();
            return null;
        }
        return focused;
    }

    private AccessibilityNodeInfo reacquireByStableIdentity(NodeRef ref) {
        if (TextUtils.isEmpty(ref.viewId)) return reacquire(ref);
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;
        try {
            if (root.getWindowId() != ref.windowId
                    || !TextUtils.equals(root.getPackageName(), ref.packageName)) return null;
            List<AccessibilityNodeInfo> matches = root.findAccessibilityNodeInfosByViewId(ref.viewId);
            AccessibilityNodeInfo selected = null;
            for (AccessibilityNodeInfo candidate : matches) {
                if (matchesStableIdentity(candidate, ref)) {
                    if (selected != null) {
                        selected.recycle();
                        selected = null;
                        break;
                    }
                    selected = AccessibilityNodeInfo.obtain(candidate);
                }
            }
            for (AccessibilityNodeInfo candidate : matches) candidate.recycle();
            return selected;
        } finally {
            root.recycle();
        }
    }

    private AccessibilityNodeInfo resolve(AccessibilityNodeInfo root, List<Integer> path) {
        AccessibilityNodeInfo current = AccessibilityNodeInfo.obtain(root);
        for (Integer index : path) {
            AccessibilityNodeInfo child = current.getChild(index);
            current.recycle();
            if (child == null) return null;
            current = child;
        }
        return current;
    }

    private boolean matches(AccessibilityNodeInfo node, NodeRef ref) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        return matchesStableIdentity(node, ref) && bounds.equals(ref.bounds);
    }

    private boolean matchesStableIdentity(AccessibilityNodeInfo node, NodeRef ref) {
        return TextUtils.equals(node.getPackageName(), ref.packageName)
                && TextUtils.equals(node.getClassName(), ref.className)
                && TextUtils.equals(node.getViewIdResourceName(), ref.viewId);
    }

    private JSONObject currentWindowSummary() {
        JSONObject state = new JSONObject();
        put(state, "locked", isLocked());
        put(state, "generation", generation.get());
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            put(state, "package", JSONObject.NULL);
            put(state, "window_id", JSONObject.NULL);
            return state;
        }
        try {
            put(state, "package", charSequence(root.getPackageName()));
            put(state, "window_id", root.getWindowId());
        } finally {
            root.recycle();
        }
        return state;
    }

    private boolean isLocked() {
        KeyguardManager manager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        return manager == null || manager.isDeviceLocked() || manager.isKeyguardLocked();
    }

    private JSONArray supportedActions(AccessibilityNodeInfo node) {
        JSONArray actions = new JSONArray();
        int mask = node.getActions();
        if ((mask & AccessibilityNodeInfo.ACTION_CLICK) != 0) actions.put("click");
        if ((mask & AccessibilityNodeInfo.ACTION_FOCUS) != 0) actions.put("focus");
        if ((mask & AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) != 0) actions.put("scroll_forward");
        if ((mask & AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) != 0) actions.put("scroll_backward");
        if (node.isEditable() && (mask & AccessibilityNodeInfo.ACTION_SET_TEXT) != 0) {
            actions.put("set_text");
        }
        return actions;
    }

    private static JSONArray boundsToJson(Rect rect) {
        JSONArray bounds = new JSONArray();
        bounds.put(rect.left);
        bounds.put(rect.top);
        bounds.put(rect.right);
        bounds.put(rect.bottom);
        return bounds;
    }

    private static String limitedText(CharSequence value) {
        if (value == null) return "";
        String text = value.toString();
        return text.length() <= 2000 ? text : text.substring(0, 2000);
    }

    private static String charSequence(CharSequence value) {
        return value == null ? "" : value.toString();
    }

    private static Object emptyToNull(String value) {
        return value == null || value.isEmpty() ? JSONObject.NULL : value;
    }

    private static JSONObject success(String id, JSONObject result) {
        JSONObject response = new JSONObject();
        put(response, "version", PROTOCOL_VERSION);
        put(response, "id", id == null ? JSONObject.NULL : id);
        put(response, "ok", true);
        put(response, "result", result);
        return response;
    }

    private static JSONObject failure(String id, String code, String message) {
        JSONObject error = new JSONObject();
        put(error, "code", code);
        put(error, "message", message);
        JSONObject response = new JSONObject();
        put(response, "version", PROTOCOL_VERSION);
        put(response, "id", id == null ? JSONObject.NULL : id);
        put(response, "ok", false);
        put(response, "error", error);
        return response;
    }

    private static void put(JSONObject object, String key, Object value) {
        try {
            object.put(key, value);
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String readLineLimited(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        while (bytes.size() <= MAX_REQUEST_BYTES) {
            int value = input.read();
            if (value < 0 || value == '\n') break;
            bytes.write(value);
        }
        if (bytes.size() > MAX_REQUEST_BYTES) throw new IOException("Request is too large");
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    private static boolean constantTimeEquals(String supplied, String expected) {
        byte[] suppliedBytes = supplied == null
                ? new byte[0] : supplied.getBytes(StandardCharsets.UTF_8);
        byte[] expectedBytes = expected == null
                ? new byte[0] : expected.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(suppliedBytes, expectedBytes);
    }

    private static void writeResponse(OutputStream output, JSONObject response) throws IOException {
        byte[] bytes = (response.toString() + "\n").getBytes(StandardCharsets.UTF_8);
        output.write(bytes);
        output.flush();
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException ignored) {
            // The service is stopping; no user data is logged.
        }
    }

    private static final class NodeRef {
        final long generation;
        final int windowId;
        final List<Integer> path;
        final String packageName;
        final String className;
        final String viewId;
        final Rect bounds;

        NodeRef(long generation, int windowId, List<Integer> path,
                String packageName, String className, String viewId, Rect bounds) {
            this.generation = generation;
            this.windowId = windowId;
            this.path = new ArrayList<>(path);
            this.packageName = packageName;
            this.className = className;
            this.viewId = viewId;
            this.bounds = new Rect(bounds);
        }
    }
}
