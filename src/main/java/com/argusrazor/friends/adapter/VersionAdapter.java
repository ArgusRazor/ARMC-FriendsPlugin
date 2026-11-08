package com.argusrazor.friends.adapter;

import org.bukkit.Bukkit;

public class VersionAdapter {

    private static final String SERVER_VERSION;
    private static final int MAJOR;
    private static final int MINOR;
    private static final int PATCH;

    static {
        String ver = "1.21.3";
        try {
            ver = Bukkit.getMinecraftVersion();
        } catch (Throwable t) {
            String full = Bukkit.getBukkitVersion();
            int dashIndex = full.indexOf('-');
            if (dashIndex > 0) {
                ver = full.substring(0, dashIndex);
            }
        }
        SERVER_VERSION = ver;

        int major = 1;
        int minor = 21;
        int patch = 0;

        try {
            String[] parts = ver.split("\\.");
            if (parts.length > 0) {
                major = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
            }
            if (parts.length > 1) {
                minor = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
            }
            if (parts.length > 2) {
                patch = Integer.parseInt(parts[2].replaceAll("[^0-9]", ""));
            }
        } catch (Exception ignored) {
        }

        MAJOR = major;
        MINOR = minor;
        PATCH = patch;
    }

    public static String getServerVersion() {
        return SERVER_VERSION;
    }

    public static int getMajor() {
        return MAJOR;
    }

    public static int getMinor() {
        return MINOR;
    }

    public static int getPatch() {
        return PATCH;
    }

    public static boolean isAtLeast(int reqMajor, int reqMinor) {
        return isAtLeast(reqMajor, reqMinor, 0);
    }

    public static boolean isAtLeast(int reqMajor, int reqMinor, int reqPatch) {
        if (MAJOR > reqMajor) return true;
        if (MAJOR < reqMajor) return false;
        if (MINOR > reqMinor) return true;
        if (MINOR < reqMinor) return false;
        return PATCH >= reqPatch;
    }

    public static boolean isMinecraft26OrHigher() {
        return MAJOR >= 26;
    }

    public static boolean hasItemModelSupport() {
        return isMinecraft26OrHigher() || isAtLeast(1, 21, 4);
    }

    public static boolean hasNativePlayerProfile() {
        return isMinecraft26OrHigher() || isAtLeast(1, 18, 2);
    }
}
