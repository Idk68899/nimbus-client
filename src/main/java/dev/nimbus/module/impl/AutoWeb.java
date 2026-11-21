package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import dev.nimbus.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

public class AutoWeb extends Module {
    private final NumberSetting range = add("General", new NumberSetting("Range", 4.5, 2, 6, 0.5));
    private final NumberSetting delay = add("General", new NumberSetting("Delay ticks", 3, 0, 10, 1));
    private int wait;

    public AutoWeb() { super("AutoWeb", "Combat", -1, "Places cobwebs on your target's feet."); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        if (wait > 0) { wait--; return; }
        if (Sync.lockedByOther(this)) return;
        LivingEntity t = Sync.target() != null ? Sync.target() : Target.find(range.get(), true, "Distance", 360);
        if (t == null || p.distanceTo(t) > range.get()) return;
        int slot = Inv.findHotbar(s -> s.is(Items.COBWEB));
        if (slot < 0) return;
        var pos = t.blockPosition();
        if (!mc.level.getBlockState(pos).canBeReplaced() || mc.level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.COBWEB)) return;
        int prev = Inv.selected();
        Inv.select(slot);
        boolean ok = Place.block(pos, InteractionHand.MAIN_HAND);
        Inv.select(prev);
        if (ok) wait = delay.get().intValue();
    }
}