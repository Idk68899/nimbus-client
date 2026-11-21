package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.BoolSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public class Esp extends Module {
    private final BoolSetting playersOnly = add(new BoolSetting("Players only", true));

    public Esp() { super("ESP", "Render", GLFW.GLFW_KEY_H); }

    @Override
    public void onTick() {
        if (mc.level == null) return;
        for (Entity e : mc.level.entitiesForRendering()) { // CHECK
            if (e == mc.player || !(e instanceof LivingEntity)) continue;
            e.setGlowingTag(!playersOnly.get() || e instanceof Player); // CHECK: setGlowingTag
        }
    }

    @Override
    public void onDisable() {
        if (mc.level == null) return;
        for (Entity e : mc.level.entitiesForRendering()) e.setGlowingTag(false);
    }
}