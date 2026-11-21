package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class Flight extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", 2.0, 1.0, 10.0, 0.5));

    public Flight() { super("Flight", "Movement", GLFW.GLFW_KEY_F); }

    @Override
    public void onTick() {
        var a = mc.player.getAbilities();
        a.mayfly = true;
        a.flying = true;
        a.setFlyingSpeed((float) (speed.get() * 0.05)); // CHECK: setFlyingSpeed
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        var a = mc.player.getAbilities();
        a.setFlyingSpeed(0.05f);
        if (!mc.player.isCreative()) { a.mayfly = false; a.flying = false; }
    }
}