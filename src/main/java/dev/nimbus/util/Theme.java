package dev.nimbus.util;

import org.lwjgl.glfw.GLFW;

public final class Theme {
    public static int color(String cat) {
        return switch (cat) {
            case "Combat" -> 0xFFE05A5A;
            case "Player" -> 0xFF5AE08A;
            case "Movement" -> 0xFF5A9BE0;
            case "Render" -> 0xFFB05AE0;
            default -> 0xFFFFFFFF;
        };
    }

    public static int alpha(int color, int a) { return (color & 0x00FFFFFF) | (a << 24); }

    public static String keyName(int k) {
        if (k < 0) return "None";
        return switch (k) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RAlt";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LAlt";
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            default -> {
                String s = GLFW.glfwGetKeyName(k, 0);
                yield s != null ? s.toUpperCase() : "K" + k;
            }
        };
    }
}