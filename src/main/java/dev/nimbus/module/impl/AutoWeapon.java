package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.BoolSetting;
import dev.nimbus.util.Inv;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.phys.EntityHitResult;

public class AutoWeapon extends Module {
    private final BoolSetting preferAxe = add(new BoolSetting("Prefer axe", false));

    public AutoWeapon() { super("AutoWeapon", "Combat", -1); }

    @Override
    public void onTick() {
        if (mc.player == null || mc.gui.screen() != null) return; // CHECK
        if (!mc.options.keyAttack.isDown() || !(mc.hitResult instanceof EntityHitResult)) return;
        int sword = Inv.findHotbar(s -> s.is(ItemTags.SWORDS)); // CHECK: tag names
        int axe = Inv.findHotbar(s -> s.is(ItemTags.AXES));
        int slot = preferAxe.get() ? (axe >= 0 ? axe : sword) : (sword >= 0 ? sword : axe);
        if (slot >= 0 && slot != Inv.selected()) Inv.select(slot);
    }
}