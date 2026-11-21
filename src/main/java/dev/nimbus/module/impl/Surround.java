package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import dev.nimbus.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class Surround extends Module {
    private static final Direction[] SIDES = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
    private final NumberSetting delay = add(new NumberSetting("Delay ticks", 1, 0, 6, 1));
    private int wait;

    public Surround() { super("Surround", "Combat", GLFW.GLFW_KEY_O); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        if (wait > 0) { wait--; return; }
        int slot = Inv.findHotbar(s -> s.is(Items.OBSIDIAN));
        if (slot < 0) return;

        BlockPos feet = p.blockPosition();
        for (Direction d : SIDES) {
            BlockPos pos = feet.relative(d);
            if (!mc.level.getBlockState(pos).canBeReplaced()) continue;
            int prev = Inv.selected();
            Inv.select(slot);
            boolean ok = Place.block(pos, InteractionHand.MAIN_HAND);
            Inv.select(prev);
            if (ok) { wait = delay.get().intValue(); return; }
        }
    }
}