package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import dev.nimbus.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

public class KillAura extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", 3.0, 1.0, 6.0, 0.1));
    private final NumberSetting fov = add(new NumberSetting("FOV", 360, 30, 360, 10));
    private final NumberSetting speed = add(new NumberSetting("Aim speed", 45, 5, 180, 5));
    private final NumberSetting jitter = add(new NumberSetting("Delay jitter", 2, 0, 6, 1));
    private final ModeSetting sort = add(new ModeSetting("Sort", "Distance", "Distance", "Health", "Angle"));
    private final BoolSetting playersOnly = add(new BoolSetting("Players only", true));
    private final BoolSetting rotate = add(new BoolSetting("Rotate", true));
    private int wait;

    public KillAura() { super("KillAura", "Combat", GLFW.GLFW_KEY_R); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        if (wait > 0) { wait--; return; }
        if (p.getAttackStrengthScale(0.5f) < 0.95f) return;

        LivingEntity t = Target.find(range.get(), playersOnly.get(), sort.get(), fov.get());
        if (t == null) return;

        if (rotate.get()) {
            Rot.smooth(Target.aim(t), speed.get().floatValue());
            if (Rot.yawDiff(Target.aim(t)) > 20) return;
        }
        mc.gameMode.attack(p, t);
        p.swing(InteractionHand.MAIN_HAND);
        wait = (int) Rand.between(0, jitter.get() + 1);
    }
}