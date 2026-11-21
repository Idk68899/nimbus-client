package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import dev.nimbus.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class SpearCharge extends Module {
    private final NumberSetting range = add("Targeting", new NumberSetting("Charge range", 8, 3, 14, 1));
    private final NumberSetting minRange = add("Targeting", new NumberSetting("Release inside", 1.5, 0.5, 4.0, 0.5));
    private final BoolSetting playersOnly = add("Targeting", new BoolSetting("Players only", true));
    private final BoolSetting shareTarget = add("Targeting", new BoolSetting("Use KillAura target", true));

    private final BoolSetting autoSwitch = add("Spear", new BoolSetting("Auto switch to spear", true));
    private final BoolSetting rotate = add("Rotation", new BoolSetting("Rotate", true));
    private final NumberSetting aimSpeed = add("Rotation", new NumberSetting("Aim speed", 40, 10, 180, 5), () -> rotate.get());
    private final BoolSetting gate = add("Rotation", new BoolSetting("Require crosshair on target", true));

    private boolean charging;

    public SpearCharge() { super("SpearCharge", "Combat", GLFW.GLFW_KEY_N, "Holds the spear charge while you run at a target, releases when it leaves range."); }

    private void release() {
        if (charging) { mc.options.keyUse.setDown(false); charging = false; }
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gui.screen() != null) { release(); return; } // CHECK
        if (Sync.lockedByOther(this)) { release(); return; }

        LivingEntity t = shareTarget.get() && Sync.target() != null && p.distanceTo(Sync.target()) <= range.get()
                ? Sync.target() : Target.find(range.get(), playersOnly.get(), "Distance", 360);
        if (t == null) { release(); return; }

        int slot = Inv.findHotbar(Gear::isSpear);
        if (slot < 0) { release(); return; }
        if (!Gear.isSpear(p.getMainHandItem())) {
            if (!autoSwitch.get()) { release(); return; }
            Inv.select(slot);
        }

        double dist = p.distanceTo(t);
        if (dist < minRange.get()) { release(); return; }

        Vec3 at = Target.aim(t);
        if (rotate.get()) Rot.smooth(at, aimSpeed.get().floatValue());
        if (gate.get() && !Rot.looking(t, 0.2, range.get() + 1)) { release(); return; }

        mc.options.keyUse.setDown(true);
        charging = true;
        Sync.setTarget(t);
    }

    @Override
    public void onDisable() { release(); }
}