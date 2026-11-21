package dev.nimbus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

public final class Build {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final Direction[] SIDES = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };

    /** Ordered placement list: feet sides first, then head sides, then the top, so each block has support. */
    public static List<BlockPos> cover(BlockPos feet, boolean feetSides, boolean headSides, boolean top) {
        List<BlockPos> l = new ArrayList<>();
        if (feetSides) for (Direction d : SIDES) l.add(feet.relative(d));
        if (headSides) for (Direction d : SIDES) l.add(feet.relative(d).above());
        if (top) l.add(feet.above(2));
        return l;
    }

    public static int blockSlot(boolean obsidianOnly) {
        int s = Inv.findHotbar(x -> x.is(Items.OBSIDIAN));
        if (s >= 0 || obsidianOnly) return s;
        return Inv.findHotbar(x -> x.getItem() instanceof BlockItem && !x.is(Items.TNT));
    }

    public static boolean complete(List<BlockPos> order) {
        for (BlockPos p : order) if (mc.level.getBlockState(p).canBeReplaced()) return false;
        return true;
    }

    public static boolean placeNext(List<BlockPos> order, int slot) {
        for (BlockPos pos : order) {
            if (!mc.level.getBlockState(pos).canBeReplaced()) continue;
            int prev = Inv.selected();
            Inv.select(slot);
            boolean ok = Place.block(pos, InteractionHand.MAIN_HAND);
            Inv.select(prev);
            if (ok) return true;
        }
        return false;
    }
}