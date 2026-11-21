package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import dev.nimbus.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class WindBoost extends Module {
    private final ModeSetting trigger = add("Trigger", new ModeSetting("Trigger", "Auto", "Auto", "Once"));
    private final NumberSetting range = add("Trigger", new NumberSetting("Target range", 8, 3, 16, 1), () -> trigger.get().equals("Auto"));
    private final BoolSetting requireMace = add("Trigger", new BoolSetting("Require mace in hotbar", true));
    private final NumberSetting cooldown = add("Timing", new NumberSetting("Cooldown ticks", 30, 5, 100, 5));

    private int stage, wait, prevSlot;
    private float prevPitch;
    private boolean pending;

    public WindBoost() { super("WindBoost", "Combat", GLFW.GLFW_KEY_B, "Throws a wind charge at your feet to launch you for a mace smash."); }

    @Override
    public void onEnable() { pending = trigger.get().equals("Once"); }

    private void finishOnce() {
        if (trigger.get().equals("Once")) { pending = false; setEnabled(false); }
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        if (wait > 0) { wait--; return; }

        switch (stage) {
            case 0 -> {
                boolean once = trigger.get().equals("Once");
                boolean go;
                if (once) go = pending;
                else {
                    if (!p.onGround()) return;
                    LivingEntity t = Sync.target() != null ? Sync.target() : Target.find(range.get(), true, "Distance", 360);
                    go = t != null && p.distanceTo(t) <= range.get();
                }
                if (!go) return;
                if (requireMace.get() && Inv.findHotbar(Gear::isMace) < 0) { finishOnce(); return; }
                int slot = Inv.findHotbar(s -> s.is(Items.WIND_CHARGE));
                if (slot < 0) { finishOnce(); return; }
                if (Sync.lockedByOther(this)) return;

                Sync.lock(this, 6);
                prevSlot = Inv.selected();
                prevPitch = p.getXRot();
                Inv.select(slot);
                p.setXRot(90f);
                stage = 1;
                wait = 1;
            }
            case 1 -> {
                mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                stage = 2;
                wait = 1;
            }
            default -> {
                Inv.select(prevSlot);
                p.setXRot(prevPitch);
                Sync.unlock(this);
                stage = 0;
                wait = cooldown.get().intValue();
                finishOnce();
            }
        }
    }

    @Override
    public void onDisable() { stage = 0; wait = 0; pending = false; Sync.unlock(this); }
}