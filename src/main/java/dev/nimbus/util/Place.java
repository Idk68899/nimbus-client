package dev.nimbus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class Place {
    private static final Minecraft mc = Minecraft.getInstance();

    /** Places the held block item at pos by clicking an adjacent solid face. */
    public static boolean block(BlockPos pos, InteractionHand hand) {
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            var st = mc.level.getBlockState(n);
            if (st.isAir() || !st.getFluidState().isEmpty()) continue;
            Vec3 hit = Vec3.atCenterOf(n).add(-d.getStepX() * 0.5, -d.getStepY() * 0.5, -d.getStepZ() * 0.5);
            mc.gameMode.useItemOn(mc.player, hand, new BlockHitResult(hit, d.getOpposite(), n, false));
            mc.player.swing(hand);
            return true;
        }
        return false;
    }

    /** Right-clicks the top face of an existing block. */
    public static void use(BlockPos pos, InteractionHand hand) {
        mc.gameMode.useItemOn(mc.player, hand, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        mc.player.swing(hand);
    }
}