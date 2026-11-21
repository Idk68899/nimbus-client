package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class Flight extends Module {
    private final ModeSetting mode = add("General", new ModeSetting("Mode", "Creative", "Creative", "Glide"));
    private final NumberSetting speed = add("General", new NumberSetting("Speed", 2.0, 1.0, 10.0, 0.5), () -> mode.get().equals("Creative"));
    private final NumberSetting glide = add("General", new NumberSetting("Fall speed", 0.05, 0.0, 0.3, 0.01), () -> mode.get().equals("Glide"));

    public Flight() { super("Flight", "Movement", GLFW.GLFW_KEY_F, "Creative-style flight or a slow glide."); }

    @Override
    public void onTick() {
        var p = mc.player;
        var a = p.getAbilities();
        if (mode.get().equals("Creative")) {
            a.mayfly = true;
            a.flying = true;
            a.setFlyingSpeed((float) (speed.get() * 0.05)); // CHECK: setFlyingSpeed
        } else if (!p.onGround()) {
            Vec3 v = p.getDeltaMovement();
            if (v.y < -glide.get()) p.setDeltaMovement(v.x, -glide.get(), v.z);
        }
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        var a = mc.player.getAbilities();
        a.setFlyingSpeed(0.05f);
        if (!mc.player.isCreative()) { a.mayfly = false; a.flying = false; }
    }
}