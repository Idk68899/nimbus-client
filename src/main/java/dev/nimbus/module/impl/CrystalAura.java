package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.module.ModuleManager;
import dev.nimbus.setting.*;
import dev.nimbus.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class CrystalAura extends Module {
    private final NumberSetting targetRange = add("Target", new NumberSetting("Target range", 10, 4, 16, 1));
    private final BoolSetting shareTarget = add("Target", new BoolSetting("Use KillAura target", true));

    private final NumberSetting placeRange = add("Place", new NumberSetting("Range", 4.5, 1, 6, 0.5));
    private final NumberSetting minDamage = add("Place", new NumberSetting("Min damage", 5, 0, 20, 0.5));
    private final NumberSetting maxSelf = add("Place", new NumberSetting("Max self damage", 6, 0, 20, 0.5));
    private final BoolSetting antiSuicide = add("Place", new BoolSetting("Anti suicide", true));
    private final NumberSetting placeDelay = add("Place", new NumberSetting("Delay ticks", 1, 0, 10, 1));
    private final BoolSetting obsidian = add("Place", new BoolSetting("Place obsidian base", true));
    private final ModeSetting swap = add("Place", new ModeSetting("Switch", "Silent", "Normal", "Silent", "None"));

    private final NumberSetting breakRange = add("Break", new NumberSetting("Range", 4.5, 1, 6, 0.5));
    private final NumberSetting breakDelay = add("Break", new NumberSetting("Delay ticks", 1, 0, 10, 1));
    private final NumberSetting breakMin = add("Break", new NumberSetting("Min damage", 2, 0, 20, 0.5));
    private final NumberSetting attempts = add("Break", new NumberSetting("Attempts", 1, 1, 3, 1));
    private final BoolSetting instantPlace = add("Break", new BoolSetting("Place same tick", true));

    private final BoolSetting popFollow = add("Pop follow-up", new BoolSetting("Follow up pops", true));
    private final NumberSetting popMin = add("Pop follow-up", new NumberSetting("Pop min damage", 2, 0, 20, 0.5), () -> popFollow.get());

    private final BoolSetting rotate = add("Rotation", new BoolSetting("Rotate", false));
    private final NumberSetting aimSpeed = add("Rotation", new NumberSetting("Aim speed", 60, 10, 180, 5), () -> rotate.get());

    private final BoolSetting requireTotem = add("Safety", new BoolSetting("Require totem", true));

    private int breakWait, placeWait;

    public CrystalAura() { super("CrystalAura", "Combat", GLFW.GLFW_KEY_C, "Places obsidian bases and crystals, breaks them for damage."); }

    private boolean safe(double self) {
        return self <= maxSelf.get() && (!antiSuicide.get() || self < Dmg.hp(mc.player) - 0.5);
    }

    private boolean aimOk(Vec3 v) {
        if (!rotate.get()) return true;
        Rot.smooth(v, aimSpeed.get().floatValue());
        return Rot.yawDiff(v) < 35;
    }

    private double score(Vec3 c, LivingEntity t, double minDmg) {
        var p = mc.player;
        if (p.getEyePosition().distanceTo(c) > placeRange.get() || c.distanceTo(t.position()) > 9) return Double.NEGATIVE_INFINITY;
        double dmg = Dmg.explosion(c, t, 6);
        if (dmg < minDmg) return Double.NEGATIVE_INFINITY;
        double self = Dmg.explosion(c, p, 6);
        if (!safe(self)) return Double.NEGATIVE_INFINITY;
        return dmg - self * 0.5;
    }

    private boolean airAbove(BlockPos base) {
        return mc.level.isEmptyBlock(base.above()) && mc.level.isEmptyBlock(base.above(2))
                && mc.level.getEntities((Entity) null, new AABB(base.above())).isEmpty();
    }

    private boolean hasSupport(BlockPos pos) {
        for (Direction d : Direction.values()) {
            var st = mc.level.getBlockState(pos.relative(d));
            if (!st.isAir() && st.getFluidState().isEmpty()) return true;
        }
        return false;
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null) return; // CHECK
        if (breakWait > 0) breakWait--;
        if (placeWait > 0) placeWait--;
        if (Sync.lockedByOther(this)) return;
        if (requireTotem.get() && !Sync.hasTotem()) return;

        LivingEntity t = shareTarget.get() && Sync.target() != null && p.distanceTo(Sync.target()) <= targetRange.get()
                ? Sync.target() : Target.find(targetRange.get(), true, "Distance", 360);
        if (t == null) return;
        Sync.setTarget(t);

        boolean pop = popFollow.get() && Sync.recentPop(t, 20);
        double minPlace = pop ? Math.min(popMin.get(), minDamage.get()) : minDamage.get();

        // ---- break
        if (breakWait <= 0) {
            EndCrystal best = null;
            double bestD = -1;
            for (Entity e : mc.level.entitiesForRendering()) { // CHECK
                if (!(e instanceof EndCrystal c) || p.distanceTo(c) > breakRange.get()) continue;
                double dt = Dmg.explosion(c.position(), t, 6);
                double ds = Dmg.explosion(c.position(), p, 6);
                if (dt >= (pop ? 0 : breakMin.get()) && safe(ds) && dt > bestD) { bestD = dt; best = c; }
            }
            if (best != null) {
                if (!aimOk(best.position())) return;
                for (int i = 0; i < attempts.get(); i++) mc.gameMode.attack(p, best);
                p.swing(InteractionHand.MAIN_HAND);
                Sync.markCrystal();
                breakWait = pop ? 0 : breakDelay.get().intValue();
                if (!instantPlace.get()) return;
            }
        }

        // ---- place
        if (placeWait > 0) return;
        boolean offhand = p.getOffhandItem().is(Items.END_CRYSTAL)
                && !ModuleManager.INSTANCE.get(AutoTotem.class).isEnabled();
        int slot = Inv.findHotbar(s -> s.is(Items.END_CRYSTAL));
        if (!offhand && slot < 0) return;

        BlockPos best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        int r = (int) Math.ceil(placeRange.get());
        BlockPos o = p.blockPosition();
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos base = o.offset(dx, dy, dz);
                    var st = mc.level.getBlockState(base);
                    if (!st.is(Blocks.OBSIDIAN) && !st.is(Blocks.BEDROCK)) continue;
                    if (!airAbove(base)) continue;
                    double s = score(new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5), t, minPlace);
                    if (s > bestScore) { bestScore = s; best = base; }
                }

        if (best != null) {
            int prev = Inv.selected();
            if (!offhand && swap.get().equals("None") && prev != slot) return;
            if (!offhand && !swap.get().equals("None")) Inv.select(slot);
            InteractionHand hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            mc.gameMode.useItemOn(p, hand, new BlockHitResult(
                    new Vec3(best.getX() + 0.5, best.getY() + 1.0, best.getZ() + 0.5), Direction.UP, best, false));
            p.swing(hand);
            if (!offhand && swap.get().equals("Silent")) Inv.select(prev);
            Sync.markCrystal();
            placeWait = pop ? 0 : placeDelay.get().intValue();
            return;
        }

        // ---- no base: put down obsidian near the target
        if (!obsidian.get()) return;
        int obi = Inv.findHotbar(s -> s.is(Items.OBSIDIAN));
        if (obi < 0) return;
        BlockPos bp = null;
        double bpScore = Double.NEGATIVE_INFINITY;
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = o.offset(dx, dy, dz);
                    if (pos.distSqr(t.blockPosition()) > 25) continue;
                    if (!mc.level.getBlockState(pos).canBeReplaced() || !hasSupport(pos) || !airAbove(pos)) continue;
                    double s = score(new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), t, minPlace);
                    if (s > bpScore) { bpScore = s; bp = pos; }
                }
        if (bp == null) return;
        int prev = Inv.selected();
        Inv.select(obi);
        boolean ok = Place.block(bp, InteractionHand.MAIN_HAND);
        Inv.select(prev);
        if (ok) placeWait = placeDelay.get().intValue();
    }

    @Override
    public void onDisable() { breakWait = 0; placeWait = 0; }
}