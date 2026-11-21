package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import org.lwjgl.glfw.GLFW;

public class Sprint extends Module {
    private final ModeSetting mode = add("General", new ModeSetting("Mode", "Legit", "Legit", "Omni"));
    private final BoolSetting pauseUsing = add("General", new BoolSetting("Pause when using item", true));

    public Sprint() { super("Sprint", "Movement", GLFW.GLFW_KEY_G, "Always sprints. Omni sprints in any direction."); }

    @Override
    public void onTick() {
        var p = mc.player;
        var o = mc.options;
        if (p.horizontalCollision || p.isShiftKeyDown() || (pauseUsing.get() && p.isUsingItem())) return;
        boolean moving = mode.get().equals("Omni")
                ? o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown()
                : o.keyUp.isDown();
        if (moving) p.setSprinting(true);
    }
}