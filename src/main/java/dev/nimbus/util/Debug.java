package dev.nimbus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.Map;

public final class Debug {
    private static final Map<String, Long> last = new HashMap<>();

    /** Prints a chat line at most once every 2 seconds per message. */
    public static void say(String who, boolean on, String msg) {
        if (!on) return;
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        String key = who + msg;
        Long l = last.get(key);
        if (l != null && Sync.tick - l < 40) return;
        last.put(key, Sync.tick);
        p.sendSystemMessage(Component.literal("[" + who + "] " + msg)); // CHECK
    }
}