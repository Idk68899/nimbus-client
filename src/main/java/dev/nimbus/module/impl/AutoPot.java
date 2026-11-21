package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import dev.nimbus.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

public class AutoPot extends Module {
    private final NumberSetting health = add("Healing", new NumberSetting("Heal below", 12, 2, 19, 1));
    private final BoolSetting speed = add("Buffs", new BoolSetting("Speed", false));
    private final BoolSetting strength = add("Buffs", new BoolSetting("Strength", false));
    private final BoolSetting regen = add("Buffs", new BoolSetting("Regeneration", false));
    private final BoolSetting fireRes = add("Buffs", new BoolSetting("Fire resistance", false));
    private final BoolSetting combatOnly = add("Buffs", new BoolSetting("Buffs only in combat", true));

    private final BoolSetting avoid = add("Throw", new BoolSetting("Don't heal enemies", true));
    private final NumberSetting enemyRadius = add("Throw", new NumberSetting("Enemy radius", 5.0, 4.0, 8.0, 0.5), () -> avoid.get());
    private final NumberSetting maxOffset = add("Throw", new NumberSetting("Max throw offset", 3.0, 1.0, 4.0, 0.5), () -> avoid.get());
    private final BoolSetting lookDown = add("Throw", new BoolSetting("Aim at ground", true));

    private final BoolSetting refill = add("Inventory", new BoolSetting("Refill from inventory", true));
    private final BoolSetting keepReady = add("Inventory", new BoolSetting("Keep a healing pot ready", true), () -> refill.get());
    private final NumberSetting cooldown = add("Timing", new NumberSetting("Cooldown ticks", 8, 2, 40, 1));

    private int stage, wait, prevSlot;
    private float prevPitch, prevYaw;
    private boolean refilled;

    public AutoPot() { super("AutoPot", "Player", -1, "Throws splash potions at your feet (or away from nearby enemies so they aren't healed). Pulls pots from your inventory."); }

    private static boolean has(ItemStack s, Holder<MobEffect> eff) {
        if (!s.is(Items.SPLASH_POTION)) return false;
        PotionContents pc = s.get(DataComponents.POTION_CONTENTS); // CHECK
        if (pc == null) return false;
        for (MobEffectInstance i : pc.getAllEffects()) // CHECK: getAllEffects()
            if (i.getEffect().value() == eff.value()) return true;
        return false;
    }

    public static boolean isHealing(ItemStack s) { return has(s, MobEffects.INSTANT_HEALTH); } // CHECK: may be HEAL

    private int slotFor(Holder<MobEffect> eff) { return Inv.findHotbar(s -> has(s, eff)); }

    private int slotOrRefill(Holder<MobEffect> eff) {
        int s = slotFor(eff);
        if (s >= 0) return s;
        if (refill.get() && Inv.refill(st -> has(st, eff))) refilled = true;
        return -1;
    }

    private int pick() {
        var p = mc.player;
        if (Dmg.hp(p) <= health.get()) return slotOrRefill(MobEffects.INSTANT_HEALTH);
        if (combatOnly.get() && Sync.target() == null) return -1;
        if (regen.get() && !p.hasEffect(MobEffects.REGENERATION)) { int s = slotOrRefill(MobEffects.REGENERATION); if (s >= 0 || refilled) return s; }
        if (speed.get() && !p.hasEffect(MobEffects.SPEED)) { int s = slotOrRefill(MobEffects.SPEED); if (s >= 0 || refilled) return s; }
        if (strength.get() && !p.hasEffect(MobEffects.STRENGTH)) { int s = slotOrRefill(MobEffects.STRENGTH); if (s >= 0 || refilled) return s; }
        if (fireRes.get() && !p.hasEffect(MobEffects.FIRE_RESISTANCE)) { int s = slotOrRefill(MobEffects.FIRE_RESISTANCE); if (s >= 0 || refilled) return s; }
        return -1;
    }

    private Player nearestEnemy(double radius) {
        Player best = null;
        double bd = radius;
        for (Entity e : mc.level.entitiesForRendering()) { // CHECK
            if (!(e instanceof Player pl) || pl == mc.player || !pl.isAlive() || pl.isSpectator() || Teams.ignored(pl)) continue;
            double d = mc.player.distanceTo(pl);
            if (d < bd) { bd = d; best = pl; }
        }
        return best;
    }

    /** Horizontal landing distance of a splash potion thrown at this pitch (approximate vanilla arc). */
    private static double land(int xRotDeg) {
        double h = 0.5 * Math.cos(Math.toRadians(xRotDeg));
        double vy = -0.5 * Math.sin(Math.toRadians(xRotDeg - 20));
        double x = 0, y = 1.62;
        for (int i = 0; i < 40; i++) {
            x += h;
            y += vy;
            h *= 0.99;
            vy = vy * 0.99 - 0.05;
            if (y <= 0) break;
        }
        return x;
    }

    private static float pitchFor(double offset) {
        float best = 90f;
        double bestErr = Double.MAX_VALUE;
        for (int a = 30; a <= 90; a++) {
            double e = Math.abs(land(a) - offset);
            if (e < bestErr) { bestErr = e; best = a; }
        }
        return best;
    }

    /** Yaw/pitch that lands the potion behind you, away from the enemy, so only you are in its splash. */
    private float[] throwAim(Player enemy) {
        var p = mc.player;
        if (!lookDown.get()) return new float[]{p.getYRot(), p.getXRot()};
        if (!avoid.get() || enemy == null) return new float[]{p.getYRot(), 90f};
        double dx = p.getX() - enemy.getX(), dz = p.getZ() - enemy.getZ();
        double d = Math.hypot(dx, dz);
        double need = Math.min(maxOffset.get(), 4.4 - d);   // splash reaches ~4 blocks
        if (need <= 0.2) return new float[]{p.getYRot(), 90f};
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        return new float[]{yaw, pitchFor(need)};
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.gameMode == null || mc.level == null) return;
        if (wait > 0) { wait--; return; }

        switch (stage) {
            case 0 -> {
                if (Sync.lockedByOther(this)) return;
                if (keepReady.get() && refill.get() && slotFor(MobEffects.INSTANT_HEALTH) < 0 && Inv.refill(AutoPot::isHealing)) {
                    wait = 2;
                    return;
                }
                refilled = false;
                int slot = pick();
                if (refilled) { refilled = false; wait = 2; return; }
                if (slot < 0) return;

                Sync.lock(this, 8);
                prevSlot = Inv.selected();
                prevYaw = p.getYRot();
                prevPitch = p.getXRot();
                Inv.select(slot);
                float[] aim = throwAim(nearestEnemy(enemyRadius.get()));
                p.setYRot(aim[0]);
                p.setXRot(aim[1]);
                stage = 1;
                wait = 1;   // let the server see the new rotation before the throw
            }
            case 1 -> {
                mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                stage = 2;
                wait = 1;
            }
            default -> {
                Inv.select(prevSlot);
                p.setYRot(prevYaw);
                p.setXRot(prevPitch);
                Sync.unlock(this);
                stage = 0;
                wait = cooldown.get().intValue();
            }
        }
    }

    @Override
    public void onDisable() { stage = 0; wait = 0; Sync.unlock(this); }
}