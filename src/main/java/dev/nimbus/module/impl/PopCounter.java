package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.BoolSetting;
import dev.nimbus.util.Sync;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;

public class PopCounter extends Module {
    private final BoolSetting self = add("General", new BoolSetting("Include yourself", true));
    private final Map<Integer, Integer> seen = new HashMap<>();

    public PopCounter() { super("PopCounter", "Player", -1, "Prints a chat line whenever a player pops a totem."); }

    @Override
    public void onTick() {
        if (mc.level == null || mc.player == null) return;
        for (Entity e : mc.level.entitiesForRendering()) { // CHECK
            if (!(e instanceof Player pl)) continue;
            if (pl == mc.player && !self.get()) continue;
            int n = Sync.pops(pl);
            if (n > seen.getOrDefault(pl.getId(), 0)) {
                seen.put(pl.getId(), n);
                mc.player.sendSystemMessage(Component.literal("[Nimbus] " + pl.getName().getString() + " popped (" + n + ")")); // CHECK
            }
        }
    }
}