package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class Speed extends Module {
    private final NumberSetting mult = add(new NumberSetting("Multiplier", 1.3, 1.0, 2.0, 0.05));

    public Speed() { super("Speed", "Movement", GLFW.GLFW_KEY_V); }

    @Override
    public void onTick() {
        var p = mc.player;
        var o = mc.options;
        boolean moving = o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown();
        if (!p.onGround() || !moving) return;
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(v.x * mult.get(), v.y, v.z * mult.get());
    }
}