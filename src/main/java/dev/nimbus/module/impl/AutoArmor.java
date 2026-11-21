package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import dev.nimbus.util.Inv;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class AutoArmor extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay ticks", 3, 0, 10, 1));
    private int wait;

    public AutoArmor() { super("AutoArmor", "Player", -1); }

    private static EquipmentSlot slotFor(ItemStack s) {
        if (s.is(ItemTags.HEAD_ARMOR)) return EquipmentSlot.HEAD;   // CHECK: tag names
        if (s.is(ItemTags.CHEST_ARMOR)) return EquipmentSlot.CHEST;
        if (s.is(ItemTags.LEG_ARMOR)) return EquipmentSlot.LEGS;
        if (s.is(ItemTags.FOOT_ARMOR)) return EquipmentSlot.FEET;
        return null;
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.gameMode == null || mc.gui.screen() != null) return; // CHECK
        if (wait > 0) { wait--; return; }

        for (int i = 0; i < 36; i++) {
            ItemStack s = p.getInventory().getItem(i);
            EquipmentSlot es = slotFor(s);
            if (es == null || !p.getItemBySlot(es).isEmpty()) continue;
            Inv.quickMove(Inv.menuSlot(i));
            wait = delay.get().intValue();
            return;
        }
    }
}