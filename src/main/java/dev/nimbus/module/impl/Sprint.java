package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import org.lwjgl.glfw.GLFW;

public class Sprint extends Module {
    public Sprint() { super("Sprint", "Movement", GLFW.GLFW_KEY_G); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (mc.options.keyUp.isDown() && !p.horizontalCollision && !p.isShiftKeyDown())
            p.setSprinting(true);
    }
}