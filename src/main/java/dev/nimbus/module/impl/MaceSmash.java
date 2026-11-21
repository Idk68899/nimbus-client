package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import dev.nimbus.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class MaceSmash extends Module {
    private final NumberSetting range = add("Targeting", new NumberSetting("Range", 4.0, 3.0, 6.0, 0.5));
    private final BoolSetting playersOnly = add("Targeting", new BoolSetting("Players only", true));
    private final BoolSetting shareTarget = add("Targeting", new BoolSetting("Use KillAura target", true));

    private final NumberSetting minFall = add("Smash", new NumberSetting("Min fall distance", 2.0, 1.5, 15.0, 0.5));
    private final BoolSetting autoSwitch = add("Smash", new BoolSetting("Auto switch to mace", true));
    private final BoolSetting switchBack = add("Smash", new BoolSetting("Switch back", true), () -> autoSwitch.get());

    private final BoolSetting rotate = add("Rotation", new BoolSetting("Rotate", true));
    private final NumberSetting aimSpeed = add("Rotation", new NumberSetting("Aim speed", 60, 10, 180, 5), () -> rotate.get());
    private final BoolSetting gate = add("Rotation", new BoolSetting("Require crosshair on target", true));

    private int prev = -1;

    public MaceSmash() { super("MaceSmash", "Combat", GLFW.GLFW_KEY_M, "Switches to the mace and smashes your target while you fall onto them."); }

    private void restore() {
        if (prev >= 0 && switchBack.get() && mc.player != null) Inv.select(prev);
        prev = -1;
        Sync.unlock(this);
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null) return; // CHECK

        boolean falling = !p.onGround() && p.fallDistance >= minFall.get() && !p.isFallFlying();
        if (!falling) { restore(); return; }
        if (Sync.lockedByOther(this)) return;

        LivingEntity t = shareTarget.get() && Sync.target() != null && p.distanceTo(Sync.target()) <= range.get()
                ? Sync.target() : Target.find(range.get(), playersOnly.get(), "Distance", 360);
        if (t == null) { restore(); return; }

        int slot = Inv.findHotbar(Gear::isMace);
        if (slot < 0) return;
        Sync.lock(this, 3);
        Sync.setTarget(t);

        if (Inv.selected() != slot) {
            if (!autoSwitch.get()) return;
            if (prev < 0) prev = Inv.selected();
            Inv.select(slot);
        }

        Vec3 at = Target.aim(t);
        if (rotate.get()) Rot.smooth(at, aimSpeed.get().floatValue());
        if (gate.get() && !Rot.looking(t, 0.1, range.get() + 1)) return;
        if (p.getAttackStrengthScale(0.5f) < 0.9f) return;

        mc.gameMode.attack(p, t);
        p.swing(InteractionHand.MAIN_HAND);
    }

    @Override
    public void onDisable() { restore(); }
}