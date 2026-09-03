package com.vallistruqui.brainrotshield;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

final class ShortFormSignals {
    private static final int ACTION_LIKE = 1;
    private static final int ACTION_DISLIKE = 1 << 1;
    private static final int ACTION_COMMENT = 1 << 2;
    private static final int ACTION_SHARE = 1 << 3;
    private static final int ACTION_REMIX = 1 << 4;
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private final ProtectedApp app;
    private boolean targetLabel;
    private boolean selectedTargetLabel;
    private boolean explicitSelectedLabel;
    private boolean strongSurface;
    private int actionMask;

    ShortFormSignals(ProtectedApp app) {
        this.app = app;
    }

    void observe(CharSequence text, CharSequence contentDescription, String viewId,
            CharSequence className, boolean selected) {
        String label = normalize(join(text, contentDescription));
        String normalizedId = normalize(viewId);
        String normalizedClass = normalize(className == null ? null : className.toString());

        boolean matchesTarget = matchesTargetLabel(label);
        targetLabel |= matchesTarget;
        selectedTargetLabel |= selected && matchesTarget;
        explicitSelectedLabel |= matchesTarget && containsAny(label,
                "selected", "seleccionado", "seleccionada", "selecionado", "selecionada");
        strongSurface |= matchesStrongSurface(normalizedId, normalizedClass);

        if (containsAny(label, "dislike", "no me gusta", "nao gostei")) {
            actionMask |= ACTION_DISLIKE;
        } else if (containsAny(label,
                "like this video", "like video", "me gusta", "curtir", "gostei")) {
            actionMask |= ACTION_LIKE;
        }
        if (containsAny(label,
                "comments", "comment", "comentarios", "comentario", "comentar")) {
            actionMask |= ACTION_COMMENT;
        }
        if (containsAny(label, "share", "compartir", "compartilhar")) {
            actionMask |= ACTION_SHARE;
        }
        if (label.contains("remix")) {
            actionMask |= ACTION_REMIX;
        }
    }

    boolean indicatesShortForm() {
        int actions = Integer.bitCount(actionMask);
        switch (app) {
            case YOUTUBE:
            case INSTAGRAM:
                return selectedTargetLabel
                        || explicitSelectedLabel
                        || (strongSurface && actions >= 1);
            case TIKTOK:
                return (selectedTargetLabel && actions >= 1)
                        || (strongSurface && actions >= 1)
                        || actions >= 3;
            default:
                return false;
        }
    }

    String summary() {
        return "app=" + app.displayName()
                + ", selected=" + selectedTargetLabel
                + ", explicitSelected=" + explicitSelectedLabel
                + ", surface=" + strongSurface
                + ", label=" + targetLabel
                + ", actions=" + Integer.bitCount(actionMask);
    }

    private boolean matchesTargetLabel(String label) {
        switch (app) {
            case YOUTUBE:
                return containsAny(label, "shorts", "youtube shorts");
            case INSTAGRAM:
                return containsAny(label, "reels", "reel");
            case TIKTOK:
                return containsAny(label,
                        "for you", "para ti", "para voce", "following", "siguiendo",
                        "seguindo", "friends", "amigos", "discover", "descubrir");
            default:
                return false;
        }
    }

    private boolean matchesStrongSurface(String viewId, String className) {
        switch (app) {
            case YOUTUBE:
                return (viewId.contains("reel") || viewId.contains("shorts"))
                        && containsAny(viewId, "player", "recycler", "pager", "watch", "feed");
            case INSTAGRAM:
                return containsAny(viewId,
                        "clips_viewer", "clips_recycler", "reels_viewer", "reel_viewer")
                        || ((viewId.contains("clips") || viewId.contains("reels"))
                        && containsAny(viewId, "pager", "recycler", "player", "video"));
            case TIKTOK:
                boolean feedId = containsAny(viewId,
                        "aweme", "feed", "viewpager", "video_pager", "recommend_feed");
                boolean pagingClass = containsAny(className,
                        "viewpager", "recyclerview", "viewpager2");
                return feedId || (pagingClass && targetLabel);
            default:
                return false;
        }
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
