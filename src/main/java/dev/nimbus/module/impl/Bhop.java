package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class Bhop extends Module {
    private final NumberSetting boost = add(new NumberSetting("Boost", 1.1, 1.0, 1.6, 0.05));

    public Bhop() { super("Bhop", "Movement", GLFW.GLFW_KEY_N); }

    @Override
    public void onTick() {
        var p = mc.player;
        var o = mc.options;
        boolean moving = o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown();
        if (!p.onGround() || !moving) return;
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(v.x * boost.get(), 0.42, v.z * boost.get());
    }
}