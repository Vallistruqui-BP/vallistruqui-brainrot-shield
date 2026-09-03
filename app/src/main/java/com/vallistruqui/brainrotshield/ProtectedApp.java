package com.vallistruqui.brainrotshield;

import java.util.Arrays;
import java.util.List;

enum ProtectedApp {
    YOUTUBE("youtube", "YouTube", "com.google.android.youtube"),
    INSTAGRAM("instagram", "Instagram", "com.instagram.android"),
    TIKTOK("tiktok", "TikTok", "com.zhiliaoapp.musically");

    static final List<ProtectedApp> ALL = Arrays.asList(values());

    private final String preferenceSuffix;
    private final String displayName;
    private final String packageName;

    ProtectedApp(String preferenceSuffix, String displayName, String packageName) {
        this.preferenceSuffix = preferenceSuffix;
        this.displayName = displayName;
        this.packageName = packageName;
    }

    String preferenceSuffix() {
        return preferenceSuffix;
    }

    String displayName() {
        return displayName;
    }

    String packageName() {
        return packageName;
    }

    static ProtectedApp fromPackage(CharSequence packageName) {
        if (packageName == null) {
            return null;
        }
        for (ProtectedApp app : values()) {
            if (app.packageName.contentEquals(packageName)) {
                return app;
            }
        }
        return null;
    }

    static String[] packageNames() {
        String[] result = new String[values().length];
        for (int index = 0; index < values().length; index++) {
            result[index] = values()[index].packageName;
        }
        return result;
    }
}
