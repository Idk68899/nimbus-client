package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import dev.nimbus.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class CrystalAura extends Module {
    private final NumberSetting targetRange = add(new NumberSetting("Target range", 10, 4, 16, 1));
    private final NumberSetting placeRange = add(new NumberSetting("Place range", 4.5, 1, 6, 0.5));
    private final NumberSetting breakRange = add(new NumberSetting("Break range", 4.5, 1, 6, 0.5));
    private final NumberSetting minDmg = add(new NumberSetting("Min damage", 4, 0, 20, 0.5));
    private final NumberSetting maxSelf = add(new NumberSetting("Max self damage", 6, 0, 20, 0.5));
    private final NumberSetting delay = add(new NumberSetting("Delay ticks", 2, 0, 10, 1));
    private final BoolSetting autoSwitch = add(new BoolSetting("Auto switch", true));
    private final BoolSetting rotate = add(new BoolSetting("Rotate", true));
    private final NumberSetting aimSpeed = add(new NumberSetting("Aim speed", 60, 10, 180, 5));
    private int wait;

    public CrystalAura() { super("CrystalAura", "Combat", GLFW.GLFW_KEY_C); }

    private boolean safe(double self) {
        var p = mc.player;
        return self <= maxSelf.get() && self < p.getHealth() + p.getAbsorptionAmount() - 1;
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        if (wait > 0) { wait--; return; }

        LivingEntity t = Target.find(targetRange.get(), true, "Distance", 360);
        if (t == null) return;

        // break
        EndCrystal bestC = null;
        double bestD = -1;
        for (Entity e : mc.level.entitiesForRendering()) { // CHECK
            if (!(e instanceof EndCrystal c) || p.distanceTo(c) > breakRange.get()) continue;
            double dmg = Dmg.estimate(c.position(), t, 6);
            double self = Dmg.estimate(c.position(), p, 6);
            if (dmg >= minDmg.get() && safe(self) && dmg > bestD) { bestD = dmg; bestC = c; }
        }
        if (bestC != null) {
            if (rotate.get()) Rot.smooth(bestC.position(), aimSpeed.get().floatValue());
            mc.gameMode.attack(p, bestC);
            p.swing(InteractionHand.MAIN_HAND);
            wait = delay.get().intValue();
            return;
        }

        // place
        InteractionHand hand = InteractionHand.OFF_HAND;
        if (!p.getOffhandItem().is(Items.END_CRYSTAL)) {
            hand = InteractionHand.MAIN_HAND;
            int slot = Inv.findHotbar(s -> s.is(Items.END_CRYSTAL));
            if (slot < 0) return;
            if (Inv.selected() != slot) {
                if (!autoSwitch.get()) return;
                Inv.select(slot);
            }
        }

        BlockPos best = null;
        double bestScore = -1e9;
        int r = (int) Math.ceil(placeRange.get());
        BlockPos origin = p.blockPosition();
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos base = origin.offset(dx, dy, dz);
                    var st = mc.level.getBlockState(base);
                    if (!st.is(Blocks.OBSIDIAN) && !st.is(Blocks.BEDROCK)) continue;
                    if (!mc.level.isEmptyBlock(base.above()) || !mc.level.isEmptyBlock(base.above(2))) continue;
                    Vec3 c = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
                    if (p.getEyePosition().distanceTo(c) > placeRange.get()) continue;
                    if (!mc.level.getEntities((Entity) null, new AABB(base.above())).isEmpty()) continue;
                    double dmg = Dmg.estimate(c, t, 6), self = Dmg.estimate(c, p, 6);
                    if (dmg < minDmg.get() || !safe(self)) continue;
                    double score = dmg - self * 0.5;
                    if (score > bestScore) { bestScore = score; best = base; }
                }
        if (best == null) return;

        mc.gameMode.useItemOn(p, hand, new BlockHitResult(
                new Vec3(best.getX() + 0.5, best.getY() + 1.0, best.getZ() + 0.5), Direction.UP, best, false));
        p.swing(hand);
        wait = delay.get().intValue();
    }
}