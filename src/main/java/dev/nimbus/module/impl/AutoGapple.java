package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import dev.nimbus.util.Inv;
import net.minecraft.world.item.Items;

public class AutoGapple extends Module {
    private final NumberSetting health = add(new NumberSetting("Health", 12, 2, 19, 1));
    private boolean eating;
    private int ticks, prev;

    public AutoGapple() { super("AutoGapple", "Player", -1); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null) return;

        if (!eating) {
            if (p.getHealth() > health.get() || p.isUsingItem()) return;
            int slot = Inv.findHotbar(s -> s.is(Items.GOLDEN_APPLE) || s.is(Items.ENCHANTED_GOLDEN_APPLE));
            if (slot < 0) return;
            prev = Inv.selected();
            Inv.select(slot);
            mc.options.keyUse.setDown(true);
            eating = true; ticks = 0;
            return;
        }
        ticks++;
        if (ticks >= 36 || (ticks > 3 && !p.isUsingItem())) finish();
    }

    private void finish() {
        mc.options.keyUse.setDown(false);
        if (mc.player != null) Inv.select(prev);
        eating = false;
    }

    @Override
    public void onDisable() { if (eating) finish(); }
}