package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import dev.nimbus.util.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;   // CHECK: package
import net.minecraft.world.entity.monster.Enemy;   // CHECK: package
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KillAura extends Module {
    private final NumberSetting range = add("Targeting", new NumberSetting("Range", 3.0, 1.0, 6.0, 0.1));
    private final BoolSetting throughWalls = add("Targeting", new BoolSetting("Through walls", false));
    private final BoolSetting players = add("Targeting", new BoolSetting("Players", true));
    private final BoolSetting hostile = add("Targeting", new BoolSetting("Hostile mobs", false));
    private final BoolSetting passive = add("Targeting", new BoolSetting("Passive mobs", false));
    private final BoolSetting invisible = add("Targeting", new BoolSetting("Invisible", true));
    private final BoolSetting ignoreBabies = add("Targeting", new BoolSetting("Ignore babies", true));
    private final ModeSetting priority = add("Targeting", new ModeSetting("Priority", "Distance", "Distance", "Health", "Angle"));
    private final NumberSetting fov = add("Targeting", new NumberSetting("FOV", 360, 30, 360, 10));
    private final ModeSetting mode = add("Targeting", new ModeSetting("Mode", "Single", "Single", "Switch"));
    private final NumberSetting maxTargets = add("Targeting", new NumberSetting("Max targets", 3, 2, 6, 1), () -> mode.get().equals("Switch"));

    private final ModeSetting timing = add("Timing", new ModeSetting("Timing", "Cooldown", "Cooldown", "CPS"));
    private final NumberSetting cooldown = add("Timing", new NumberSetting("Cooldown", 0.95, 0.5, 1.0, 0.05), () -> timing.get().equals("Cooldown"));
    private final NumberSetting jitter = add("Timing", new NumberSetting("Delay jitter", 1, 0, 6, 1), () -> timing.get().equals("Cooldown"));
    private final NumberSetting minCps = add("Timing", new NumberSetting("Min CPS", 8, 1, 20, 1), () -> timing.get().equals("CPS"));
    private final NumberSetting maxCps = add("Timing", new NumberSetting("Max CPS", 12, 1, 20, 1), () -> timing.get().equals("CPS"));
    private final NumberSetting hurtTime = add("Timing", new NumberSetting("Max hurt time", 10, 0, 10, 1));
    private final BoolSetting critOnly = add("Timing", new BoolSetting("Crits only (falling)", false));
    private final BoolSetting pauseCrystals = add("Timing", new BoolSetting("Pause for crystals", true));
    private final BoolSetting pauseUsing = add("Timing", new BoolSetting("Pause when using item", true));

    private final BoolSetting onlyWeapon = add("Weapon", new BoolSetting("Only weapon", false));
    private final BoolSetting autoWeapon = add("Weapon", new BoolSetting("Auto weapon", false));
    private final BoolSetting breakShields = add("Weapon", new BoolSetting("Axe vs shields", true));

    private final ModeSetting rotation = add("Rotation", new ModeSetting("Rotation", "Smooth", "Smooth", "Instant", "None"));
    private final ModeSetting aim = add("Rotation", new ModeSetting("Aim point", "Chest", "Eyes", "Chest", "Feet"));
    private final NumberSetting aimSpeed = add("Rotation", new NumberSetting("Max speed", 40, 5, 180, 5), () -> rotation.get().equals("Smooth"));
    private final NumberSetting smoothing = add("Rotation", new NumberSetting("Smoothing", 0.35, 0.05, 1.0, 0.05), () -> rotation.get().equals("Smooth"));
    private final NumberSetting noise = add("Rotation", new NumberSetting("Noise", 0.1, 0.0, 1.0, 0.05), () -> rotation.get().equals("Smooth"));
    private final ModeSetting gate = add("Rotation", new ModeSetting("Attack gate", "Raycast", "Raycast", "Angle", "None"));
    private final NumberSetting gateExpand = add("Rotation", new NumberSetting("Hitbox expand", 0.1, 0.0, 0.5, 0.05), () -> gate.get().equals("Raycast"));
    private final NumberSetting maxAngle = add("Rotation", new NumberSetting("Max angle", 25, 5, 180, 5), () -> gate.get().equals("Angle"));

    private int wait, switchIdx;
    private double cpsAcc;

    public KillAura() { super("KillAura", "Combat", GLFW.GLFW_KEY_R, "Attacks entities around you. Rotates smoothly; attacks once your crosshair is on target."); }

    private static boolean isWeapon(ItemStack s) { return s.is(ItemTags.SWORDS) || s.is(ItemTags.AXES); } // CHECK tags

    private Vec3 aim(LivingEntity e) {
        return switch (aim.get()) {
            case "Eyes" -> e.getEyePosition();
            case "Feet" -> e.position().add(0, 0.2, 0);
            default -> e.position().add(0, e.getBbHeight() * 0.6, 0);
        };
    }

    private boolean valid(Player p, LivingEntity le) {
        if (le == p || !le.isAlive()) return false;
        if (Teams.ignored(le)) return false;
        if (le instanceof Player pl) {
            if (!players.get() || pl.isCreative() || pl.isSpectator()) return false;
        } else if (le instanceof Enemy) {
            if (!hostile.get()) return false;
        } else if (le instanceof Animal) {
            if (!passive.get()) return false;
        } else return false;
        if (!invisible.get() && le.isInvisible()) return false;
        if (ignoreBabies.get() && le.isBaby()) return false;
        if (p.distanceTo(le) > range.get()) return false;
        if (!throughWalls.get() && !p.hasLineOfSight(le)) return false;
        return fov.get() >= 360 || Rot.yawDiff(aim(le)) <= fov.get() / 2.0;
    }

    private Comparator<LivingEntity> comparator(Player p) {
        return switch (priority.get()) {
            case "Health" -> Comparator.comparingDouble((LivingEntity e) -> e.getHealth());
            case "Angle" -> Comparator.comparingDouble((LivingEntity e) -> Rot.yawDiff(aim(e)));
            default -> Comparator.comparingDouble((LivingEntity e) -> p.distanceTo(e));
        };
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null) return; // CHECK
        if (Sync.lockedByOther(this) || (pauseCrystals.get() && Sync.crystalActive())) return;
        if (pauseUsing.get() && p.isUsingItem()) return;

        List<LivingEntity> list = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) { // CHECK
            if (e instanceof LivingEntity le && valid(p, le)) list.add(le);
        }
        if (list.isEmpty()) { cpsAcc = 0; return; }
        list.sort(comparator(p));

        LivingEntity t = mode.get().equals("Switch")
                ? list.get(switchIdx % Math.min(list.size(), maxTargets.get().intValue()))
                : list.get(0);
        Sync.setTarget(t);

        if (breakShields.get() && t instanceof Player pl && pl.isBlocking()) {
            int axe = Inv.findHotbar(s -> s.is(ItemTags.AXES));
            if (axe >= 0 && axe != Inv.selected()) Inv.select(axe);
        } else if (autoWeapon.get() && !isWeapon(p.getMainHandItem())) {
            int s = Inv.findHotbar(KillAura::isWeapon);
            if (s >= 0) Inv.select(s);
        }
        if (onlyWeapon.get() && !isWeapon(p.getMainHandItem())) return;

        Vec3 at = aim(t);
        switch (rotation.get()) {
            case "Smooth" -> Rot.smooth(at, aimSpeed.get().floatValue(), smoothing.get().floatValue(), noise.get().floatValue());
            case "Instant" -> Rot.smooth(at, 360f, 1f, 0f);
            default -> {}
        }

        switch (gate.get()) {
            case "Raycast" -> { if (!Rot.looking(t, gateExpand.get(), range.get() + 1)) return; }
            case "Angle" -> { if (Rot.yawDiff(at) > maxAngle.get()) return; }
            default -> {}
        }

        if (t.hurtTime > hurtTime.get()) return;
        if (critOnly.get() && !(p.fallDistance > 0 && !p.onGround() && !p.isInWater())) return; // CHECK fallDistance

        boolean cps = timing.get().equals("CPS");
        if (cps) {
            double lo = Math.min(minCps.get(), maxCps.get()), hi = Math.max(minCps.get(), maxCps.get());
            cpsAcc += Rand.between(lo, hi) / 20.0;
            if (cpsAcc < 1) return;
            cpsAcc -= 1;
        } else {
            if (wait > 0) { wait--; return; }
            if (p.getAttackStrengthScale(0.5f) < cooldown.get()) return;
        }

        mc.gameMode.attack(p, t);
        p.swing(InteractionHand.MAIN_HAND);
        switchIdx++;
        if (!cps) wait = (int) Rand.between(0, jitter.get() + 1);
    }

    @Override
    public void onDisable() { wait = 0; cpsAcc = 0; }
}