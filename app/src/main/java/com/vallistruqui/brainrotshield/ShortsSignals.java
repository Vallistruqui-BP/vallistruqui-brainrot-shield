package com.vallistruqui.brainrotshield;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

final class ShortsSignals {
    private static final int ACTION_LIKE = 1;
    private static final int ACTION_DISLIKE = 1 << 1;
    private static final int ACTION_COMMENT = 1 << 2;
    private static final int ACTION_SHARE = 1 << 3;
    private static final int ACTION_REMIX = 1 << 4;
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private boolean shortsLabel;
    private boolean selectedShorts;
    private boolean explicitSelectedLabel;
    private boolean reelSurface;
    private int actionMask;

    void observe(CharSequence text, CharSequence contentDescription, String viewId, boolean selected) {
        String label = normalize(join(text, contentDescription));
        String normalizedId = normalize(viewId);

        if (containsShorts(label) || containsShorts(normalizedId)) {
            shortsLabel = true;
        }
        if (selected && containsShorts(label)) {
            selectedShorts = true;
        }
        if (containsShorts(label) && containsAny(label,
                "selected", "seleccionado", "seleccionada", "selecionado", "selecionada")) {
            explicitSelectedLabel = true;
        }

        boolean reelId = normalizedId.contains("reel")
                && containsAny(normalizedId, "player", "recycler", "pager", "watch", "feed");
        boolean shortsSurfaceId = normalizedId.contains("shorts")
                && containsAny(normalizedId, "player", "recycler", "pager", "watch", "feed");
        reelSurface |= reelId || shortsSurfaceId;

        if (containsAny(label, "dislike", "no me gusta", "nao gostei")) {
            actionMask |= ACTION_DISLIKE;
        } else if (containsAny(label, "like this video", "me gusta", "gostei")) {
            actionMask |= ACTION_LIKE;
        }
        if (containsAny(label, "comments", "comment", "comentarios", "comentario")) {
            actionMask |= ACTION_COMMENT;
        }
        if (containsAny(label, "share", "compartir", "compartilhar")) {
            actionMask |= ACTION_SHARE;
        }
        if (label.contains("remix")) {
            actionMask |= ACTION_REMIX;
        }
    }

    boolean indicatesShorts() {
        int actionCount = Integer.bitCount(actionMask);
        return selectedShorts
                || explicitSelectedLabel
                || (reelSurface && actionCount >= 1)
                || (shortsLabel && actionCount >= 3);
    }

    String summary() {
        return "selected=" + selectedShorts
                + ", explicitSelected=" + explicitSelectedLabel
                + ", reelSurface=" + reelSurface
                + ", shortsLabel=" + shortsLabel
                + ", actions=" + Integer.bitCount(actionMask);
    }

    private static boolean containsShorts(String value) {
        return value.contains("shorts") || value.contains("youtube shorts");
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String join(CharSequence first, CharSequence second) {
        String firstValue = first == null ? "" : first.toString();
        String secondValue = second == null ? "" : second.toString();
        return firstValue + " " + secondValue;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed)
                .replaceAll("")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
