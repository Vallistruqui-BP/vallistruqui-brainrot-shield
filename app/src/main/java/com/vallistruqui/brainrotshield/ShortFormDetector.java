package com.vallistruqui.brainrotshield;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.Deque;

final class ShortFormDetector {
    private static final int MAX_NODES = 1_200;

    private ShortFormDetector() {
    }

    @SuppressWarnings("deprecation")
    static ShortFormSignals inspect(ProtectedApp app, AccessibilityNodeInfo root) {
        ShortFormSignals signals = new ShortFormSignals(app);
        if (root == null) {
            return signals;
        }

        Deque<NodeEntry> pending = new ArrayDeque<>();
        pending.push(new NodeEntry(root, false));
        int inspected = 0;

        while (!pending.isEmpty() && inspected < MAX_NODES) {
            NodeEntry entry = pending.pop();
            AccessibilityNodeInfo node = entry.node;
            inspected++;

            try {
                if (node.isVisibleToUser()) {
                    signals.observe(
                            node.getText(),
                            node.getContentDescription(),
                            node.getViewIdResourceName(),
                            node.getClassName(),
                            node.isSelected());
                }

                for (int index = node.getChildCount() - 1; index >= 0; index--) {
                    AccessibilityNodeInfo child = node.getChild(index);
                    if (child != null) {
                        pending.push(new NodeEntry(child, true));
                    }
                }
            } finally {
                if (entry.recycleAfterUse) {
                    node.recycle();
                }
            }
        }

        while (!pending.isEmpty()) {
            NodeEntry entry = pending.pop();
            if (entry.recycleAfterUse) {
                entry.node.recycle();
            }
        }
        return signals;
    }

    private static final class NodeEntry {
        private final AccessibilityNodeInfo node;
        private final boolean recycleAfterUse;

        private NodeEntry(AccessibilityNodeInfo node, boolean recycleAfterUse) {
            this.node = node;
            this.recycleAfterUse = recycleAfterUse;
        }
    }
}
