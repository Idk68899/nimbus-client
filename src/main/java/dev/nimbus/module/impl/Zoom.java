package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class Zoom extends Module {
    private final NumberSetting fov = add(new NumberSetting("FOV", 30, 30, 90, 5));
    private int prev = 70;

    public Zoom() { super("Zoom", "Render", GLFW.GLFW_KEY_Y); }

    @Override
    public void onEnable() { prev = mc.options.fov().get(); } // CHECK: options.fov()

    @Override
    public void onTick() { mc.options.fov().set(fov.get().intValue()); }

    @Override
    public void onDisable() { mc.options.fov().set(prev); }
}