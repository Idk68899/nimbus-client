package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import dev.nimbus.util.Inv;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoEat extends Module {
    private final NumberSetting hunger = add(new NumberSetting("Hunger", 14, 1, 19, 1));
    private boolean eating;
    private int ticks, prev;

    public AutoEat() { super("AutoEat", "Player", -1); }

    private static boolean good(ItemStack s) {
        return s.has(DataComponents.FOOD) // CHECK: DataComponents.FOOD
                && !s.is(Items.ROTTEN_FLESH) && !s.is(Items.PUFFERFISH)
                && !s.is(Items.SPIDER_EYE) && !s.is(Items.POISONOUS_POTATO)
                && !s.is(Items.CHORUS_FRUIT);
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null) return;

        if (!eating) {
            if (p.getFoodData().getFoodLevel() > hunger.get() || p.isUsingItem()) return; // CHECK
            int slot = Inv.findHotbar(AutoEat::good);
            if (slot < 0) return;
            prev = Inv.selected();
            Inv.select(slot);
            mc.options.keyUse.setDown(true);
            eating = true; ticks = 0;
            return;
        }
        ticks++;
        if (ticks >= 40 || (ticks > 3 && !p.isUsingItem())) finish();
    }

    private void finish() {
        mc.options.keyUse.setDown(false);
        if (mc.player != null) Inv.select(prev);
        eating = false;
    }

    @Override
    public void onDisable() { if (eating) finish(); }
}