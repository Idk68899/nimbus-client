package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import dev.nimbus.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoXP extends Module {
    private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    private final NumberSetting threshold = add("General", new NumberSetting("Repair below %", 70, 10, 99, 5));
    private final NumberSetting burst = add("General", new NumberSetting("Bottles per burst", 3, 1, 10, 1));
    private final NumberSetting delay = add("General", new NumberSetting("Delay ticks", 1, 0, 10, 1));
    private final NumberSetting cooldown = add("General", new NumberSetting("Cooldown ticks", 20, 0, 100, 5));
    private final BoolSetting noTarget = add("Safety", new BoolSetting("Only when no target nearby", true));

    private int stage, wait, prevSlot, thrown;
    private float prevPitch;

    public AutoXP() { super("AutoXP", "Player", -1, "Throws XP bottles at your feet to mend worn armor."); }

    private boolean needed() {
        for (EquipmentSlot es : ARMOR) {
            ItemStack s = mc.player.getItemBySlot(es);
            if (s.isEmpty() || s.getMaxDamage() <= 0) continue;
            double pct = 100.0 * (1.0 - (double) s.getDamageValue() / s.getMaxDamage());
            if (pct < threshold.get()) return true;
        }
        return false;
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.gameMode == null) return;
        if (wait > 0) { wait--; return; }

        switch (stage) {
            case 0 -> {
                if (!needed() || Sync.lockedByOther(this)) return;
                if (noTarget.get() && Sync.target() != null) return;
                int slot = Inv.findHotbar(s -> s.is(Items.EXPERIENCE_BOTTLE));
                if (slot < 0) return;
                Sync.lock(this, 12);
                prevSlot = Inv.selected();
                prevPitch = p.getXRot();
                Inv.select(slot);
                p.setXRot(90f);
                thrown = 0;
                stage = 1;
                wait = 1;
            }
            case 1 -> {
                mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                thrown++;
                if (thrown >= burst.get() || !needed()) { stage = 2; wait = 1; }
                else wait = delay.get().intValue();
            }
            default -> {
                Inv.select(prevSlot);
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