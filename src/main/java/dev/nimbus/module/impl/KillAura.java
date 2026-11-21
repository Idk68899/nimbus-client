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
    // targeting
    private final NumberSetting range = add(new NumberSetting("Range", 3.0, 1.0, 6.0, 0.1));
    private final BoolSetting throughWalls = add(new BoolSetting("Through walls", false));
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting hostile = add(new BoolSetting("Hostile mobs", false));
    private final BoolSetting passive = add(new BoolSetting("Passive mobs", false));
    private final BoolSetting invisible = add(new BoolSetting("Invisible", true));
    private final BoolSetting ignoreBabies = add(new BoolSetting("Ignore babies", true));
    private final ModeSetting priority = add(new ModeSetting("Priority", "Distance", "Distance", "Health", "Angle"));
    private final NumberSetting fov = add(new NumberSetting("FOV", 360, 30, 360, 10));
    private final ModeSetting mode = add(new ModeSetting("Mode", "Single", "Single", "Switch"));
    private final NumberSetting maxTargets = add(new NumberSetting("Max targets", 3, 2, 6, 1));

    // attack
    private final ModeSetting timing = add(new ModeSetting("Timing", "Cooldown", "Cooldown", "CPS"));
    private final NumberSetting cooldown = add(new NumberSetting("Cooldown", 0.95, 0.5, 1.0, 0.05));
    private final NumberSetting minCps = add(new NumberSetting("Min CPS", 8, 1, 20, 1));
    private final NumberSetting maxCps = add(new NumberSetting("Max CPS", 12, 1, 20, 1));
    private final NumberSetting jitter = add(new NumberSetting("Delay jitter", 2, 0, 6, 1));
    private final NumberSetting hurtTime = add(new NumberSetting("Max hurt time", 10, 0, 10, 1));
    private final BoolSetting onlyWeapon = add(new BoolSetting("Only weapon", false));
    private final BoolSetting autoWeapon = add(new BoolSetting("Auto weapon", false));
    private final BoolSetting pauseEating = add(new BoolSetting("Pause when eating", true));

    // rotation
    private final ModeSetting rotation = add(new ModeSetting("Rotation", "Smooth", "Smooth", "Instant", "None"));
    private final ModeSetting aim = add(new ModeSetting("Aim point", "Chest", "Eyes", "Chest", "Feet"));
    private final NumberSetting aimSpeed = add(new NumberSetting("Aim speed", 45, 5, 180, 5));
    private final NumberSetting maxAngle = add(new NumberSetting("Attack angle", 25, 5, 180, 5));

    private int wait, switchIdx;
    private double cpsAcc;

    public KillAura() { super("KillAura", "Combat", GLFW.GLFW_KEY_R); }

    private static boolean isWeapon(ItemStack s) {
        return s.is(ItemTags.SWORDS) || s.is(ItemTags.AXES); // CHECK: tag names
    }

    private Vec3 aim(LivingEntity e) {
        return switch (aim.get()) {
            case "Eyes" -> e.getEyePosition();
            case "Feet" -> e.position().add(0, 0.2, 0);
            default -> e.position().add(0, e.getBbHeight() * 0.6, 0);
        };
    }

    private boolean valid(Player p, LivingEntity le) {
        if (le == p || !le.isAlive()) return false;
        if (le instanceof Player pl) {
            if (!players.get() || pl.isCreative() || pl.isSpectator()) return false;
        } else if (le instanceof Enemy) {
            if (!hostile.get()) return false;
        } else if (le instanceof Animal) {
            if (!passive.get()) return false;
        } else {
            return false;
        }
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
        if (pauseEating.get() && p.isUsingItem()) return;

        List<LivingEntity> list = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) { // CHECK
            if (e instanceof LivingEntity le && valid(p, le)) list.add(le);
        }
        if (list.isEmpty()) { cpsAcc = 0; return; }
        list.sort(comparator(p));

        LivingEntity t = mode.get().equals("Switch")
                ? list.get(switchIdx % Math.min(list.size(), maxTargets.get().intValue()))
                : list.get(0);

        if (!isWeapon(p.getMainHandItem())) {
            if (autoWeapon.get()) {
                int s = Inv.findHotbar(KillAura::isWeapon);
                if (s >= 0) Inv.select(s);
            }
            if (onlyWeapon.get() && !isWeapon(p.getMainHandItem())) return;
        }

        Vec3 at = aim(t);
        boolean rotate = !rotation.get().equals("None");
        if (rotate) {
            Rot.smooth(at, rotation.get().equals("Instant") ? 360f : aimSpeed.get().floatValue());
            if (Rot.yawDiff(at) > maxAngle.get()) return;
        }
        if (t.hurtTime > hurtTime.get()) return;

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