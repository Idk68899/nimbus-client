package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import dev.nimbus.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class AnchorAura extends Module {
    private final NumberSetting targetRange = add(new NumberSetting("Target range", 8, 3, 14, 1));
    private final NumberSetting range = add(new NumberSetting("Range", 4.5, 1, 6, 0.5));
    private final NumberSetting charges = add(new NumberSetting("Charges", 1, 1, 4, 1));
    private final NumberSetting minDmg = add(new NumberSetting("Min damage", 4, 0, 20, 0.5));
    private final NumberSetting maxSelf = add(new NumberSetting("Max self damage", 6, 0, 20, 0.5));
    private final NumberSetting delay = add(new NumberSetting("Delay ticks", 2, 0, 10, 1));
    private int wait;

    public AnchorAura() { super("AnchorAura", "Combat", GLFW.GLFW_KEY_Z); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        if (wait > 0) { wait--; return; }

        LivingEntity t = Target.find(targetRange.get(), true, "Distance", 360);
        if (t == null) return;

        // existing anchor near the target
        BlockPos anchor = null;
        int r = (int) Math.ceil(range.get());
        BlockPos o = p.blockPosition();
        for (int dx = -r; dx <= r && anchor == null; dx++)
            for (int dy = -r; dy <= r && anchor == null; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = o.offset(dx, dy, dz);
                    if (!mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)) continue;
                    Vec3 c = Vec3.atCenterOf(pos);
                    if (p.getEyePosition().distanceTo(c) > range.get()) continue;
                    if (c.distanceTo(t.position()) > 3.5) continue;
                    anchor = pos;
                    break;
                }

        if (anchor == null) { placeAnchor(t); return; }

        Vec3 c = Vec3.atCenterOf(anchor);
        int charge = mc.level.getBlockState(anchor).getValue(RespawnAnchorBlock.CHARGE);

        if (charge < charges.get()) {
            int slot = Inv.findHotbar(s -> s.is(Items.GLOWSTONE));
            if (slot < 0) return;
            Inv.select(slot);
            Place.use(anchor, InteractionHand.MAIN_HAND);
            wait = delay.get().intValue();
            return;
        }

        if (Dmg.estimate(c, t, 5) < minDmg.get() || Dmg.estimate(c, p, 5) > maxSelf.get()) return;
        int slot = Inv.findHotbar(s -> !s.isEmpty() && !s.is(Items.GLOWSTONE) && !s.is(Items.RESPAWN_ANCHOR));
        if (slot >= 0) Inv.select(slot);
        Place.use(anchor, InteractionHand.MAIN_HAND);
        wait = delay.get().intValue();
    }

    private void placeAnchor(LivingEntity t) {
        var p = mc.player;
        int slot = Inv.findHotbar(s -> s.is(Items.RESPAWN_ANCHOR));
        if (slot < 0) return;

        BlockPos best = null;
        double bestScore = -1e9;
        BlockPos tp = t.blockPosition();
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = 0; dy <= 2; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = tp.offset(dx, dy, dz);
                    if (!mc.level.isEmptyBlock(pos)) continue;
                    Vec3 c = Vec3.atCenterOf(pos);
                    if (p.getEyePosition().distanceTo(c) > range.get()) continue;
                    double score = Dmg.estimate(c, t, 5) - Dmg.estimate(c, p, 5);
                    if (Dmg.estimate(c, p, 5) > maxSelf.get()) continue;
                    if (score > bestScore) { bestScore = score; best = pos; }
                }
        if (best == null) return;
        Inv.select(slot);
        if (Place.block(best, InteractionHand.MAIN_HAND)) wait = delay.get().intValue();
    }
}