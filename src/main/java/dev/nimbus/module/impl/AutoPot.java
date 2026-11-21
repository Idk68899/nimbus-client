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
    private final BoolSetting refill = add("Inventory", new BoolSetting("Refill from inventory", true));
    private final BoolSetting keepReady = add("Inventory", new BoolSetting("Keep a healing pot ready", true), () -> refill.get());
    private final NumberSetting cooldown = add("Timing", new NumberSetting("Cooldown ticks", 8, 2, 40, 1));
    private final BoolSetting lookDown = add("Timing", new BoolSetting("Throw at feet", true));

    private int stage, wait, prevSlot;
    private float prevPitch;
    private boolean refilled;

    public AutoPot() { super("AutoPot", "Player", -1, "Throws splash potions at your feet: healing when low, buffs in combat. Pulls pots from your inventory."); }

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

    /** Slot of the potion to throw right now, or -1. */
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

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.gameMode == null) return;
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
                prevPitch = p.getXRot();
                Inv.select(slot);
                if (lookDown.get()) p.setXRot(90f);
                stage = 1;
                wait = 1;   // let the server see the new pitch before the throw
            }
            case 1 -> {
                mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                stage = 2;
                wait = 1;
            }
            default -> {
                Inv.select(prevSlot);
                if (lookDown.get()) p.setXRot(prevPitch);
                Sync.unlock(this);
                stage = 0;
                wait = cooldown.get().intValue();
            }
        }
    }

    @Override
    public void onDisable() { stage = 0; wait = 0; Sync.unlock(this); }
}