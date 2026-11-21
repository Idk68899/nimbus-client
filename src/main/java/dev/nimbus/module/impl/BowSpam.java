package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import net.minecraft.world.item.Items;

public class BowSpam extends Module {
    private final NumberSetting charge = add(new NumberSetting("Charge ticks", 3, 1, 20, 1));

    public BowSpam() { super("BowSpam", "Combat", -1); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.gameMode == null) return;
        if (p.isUsingItem() && p.getUseItem().is(Items.BOW) && p.getTicksUsingItem() >= charge.get()) {
            mc.gameMode.releaseUsingItem(p);
        }
    }
}