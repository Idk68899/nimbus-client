package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.module.ModuleManager;
import dev.nimbus.setting.*;
import dev.nimbus.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class AnchorAura extends Module {
    private final NumberSetting targetRange = add("Target", new NumberSetting("Target range", 8, 3, 14, 1));
    private final BoolSetting shareTarget = add("Target", new BoolSetting("Use KillAura target", true));

    private final NumberSetting range = add("Anchor", new NumberSetting("Range", 4.5, 1, 6, 0.5));
    private final NumberSetting charges = add("Anchor", new NumberSetting("Charges", 1, 1, 4, 1));
    private final NumberSetting minDamage = add("Anchor", new NumberSetting("Min damage", 5, 0, 20, 0.5));
    private final NumberSetting facePlaceHp = add("Anchor", new NumberSetting("Go for kill below HP", 8, 0, 20, 1));
    private final NumberSetting maxSelf = add("Anchor", new NumberSetting("Max self damage", 6, 0, 20, 0.5));
    private final BoolSetting antiSuicide = add("Anchor", new BoolSetting("Anti suicide", true));
    private final BoolSetting killOverride = add("Anchor", new BoolSetting("Kill override (ignore self cap if it kills)", true));
    private final NumberSetting delay = add("Anchor", new NumberSetting("Delay ticks", 1, 0, 10, 1));

    private final BoolSetting refill = add("Inventory", new BoolSetting("Refill from inventory", true));

    private final BoolSetting popFollow = add("Pop follow-up", new BoolSetting("Follow up pops", true));
    private final NumberSetting popMin = add("Pop follow-up", new NumberSetting("Pop min damage", 2, 0, 20, 0.5), () -> popFollow.get());

    private final BoolSetting requireCover = add("Safety", new BoolSetting("Require SafeAnchor cover", false));
    private final BoolSetting requireTotem = add("Safety", new BoolSetting("Require totem", true));
    private final BoolSetting debug = add("Debug", new BoolSetting("Chat debug", false));

    private int wait;

    public AnchorAura() { super("AnchorAura", "Combat", GLFW.GLFW_KEY_Z, "Places, charges and detonates respawn anchors (outside the Nether)."); }

    private double reach() { return Math.min(range.get(), mc.player.blockInteractionRange()); } // CHECK

    private boolean safe(double self) {
        return self <= maxSelf.get() && (!antiSuicide.get() || self < Dmg.hp(mc.player) - 0.5);
    }

    private static boolean hasTotem(LivingEntity e) {
        return e.getOffhandItem().is(Items.TOTEM_OF_UNDYING) || e.getMainHandItem().is(Items.TOTEM_OF_UNDYING);
    }

    private boolean hasSupport(BlockPos pos) {
        for (Direction d : Direction.values()) {
            var st = mc.level.getBlockState(pos.relative(d));
            if (!st.isAir() && st.getFluidState().isEmpty()) return true;
        }
        return false;
    }

    private void useWith(BlockPos anchor, int slot) {
        int prev = Inv.selected();
        Inv.select(slot);
        Place.use(anchor, InteractionHand.MAIN_HAND);
        Inv.select(prev);
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null) return; // CHECK
        if (mc.level.dimension() == Level.NETHER) { Debug.say(name, debug.get(), "anchors don't explode in the Nether"); return; }
        if (wait > 0) { wait--; return; }
        if (Sync.lockedByOther(this)) return;
        if (requireTotem.get() && !Sync.hasTotem()) { Debug.say(name, debug.get(), "no totem (Require totem is on)"); return; }

        LivingEntity t = shareTarget.get() && Sync.target() != null && p.distanceTo(Sync.target()) <= targetRange.get()
                ? Sync.target() : Target.find(targetRange.get(), true, "Distance", 360);
        if (t == null) { Debug.say(name, debug.get(), "no target within " + targetRange.get().intValue() + " blocks (is Teams skipping them?)"); return; }
        Sync.setTarget(t);

        boolean pop = popFollow.get() && Sync.recentPop(t, 20);
        boolean kill = Dmg.hp(t) <= facePlaceHp.get();
        double minD = minDamage.get();
        if (pop) minD = Math.min(minD, popMin.get());
        if (kill) minD = Math.min(minD, 1.0);

        BlockPos anchor = null;
        double bestD = -1;
        int r = (int) Math.ceil(reach());
        BlockPos o = p.blockPosition();
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = o.offset(dx, dy, dz);
                    if (!mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)) continue;
                    Vec3 c = Vec3.atCenterOf(pos);
                    if (p.getEyePosition().distanceTo(c) > reach() || c.distanceTo(t.position()) > 6) continue;
                    double d = Dmg.explosion(c, t, 5);
                    if (d > bestD) { bestD = d; anchor = pos; }
                }

        if (anchor == null) { placeAnchor(t, minD, r); return; }

        int charge = mc.level.getBlockState(anchor).getValue(RespawnAnchorBlock.CHARGE);
        if (charge < charges.get()) {
            int gs = Inv.findHotbar(s -> s.is(Items.GLOWSTONE));
            if (gs < 0) {
                if (refill.get() && Inv.refill(s -> s.is(Items.GLOWSTONE))) { wait = 2; return; }
                Debug.say(name, debug.get(), "no glowstone in hotbar to charge the anchor");
                return;
            }
            useWith(anchor, gs);
            Sync.markCrystal();
            wait = pop ? 0 : delay.get().intValue();
            return;
        }

        Vec3 c = Vec3.atCenterOf(anchor);
        double dt = Dmg.explosion(c, t, 5), ds = Dmg.explosion(c, p, 5);
        boolean kills = !hasTotem(t) && dt >= Dmg.hp(t);
        boolean ok = (dt >= minD && safe(ds)) || (killOverride.get() && kills && ds < Dmg.hp(p) - 0.5);
        if (!ok) {
            Debug.say(name, debug.get(), String.format("holding: target dmg %.1f (min %.1f), self dmg %.1f (max %.1f), kills=%b", dt, minD, ds, maxSelf.get(), kills));
            return;
        }
        if (requireCover.get()) {
            SafeAnchor sa = ModuleManager.INSTANCE.get(SafeAnchor.class);
            if (sa == null || !sa.isEnabled() || !sa.covered()) { Debug.say(name, debug.get(), "SafeAnchor cover not complete"); return; }
        }
        int blank = Inv.findHotbar(s -> s.isEmpty() || (!s.is(Items.GLOWSTONE) && !s.is(Items.RESPAWN_ANCHOR)));
        if (blank < 0) { Debug.say(name, debug.get(), "need a hotbar slot without glowstone/anchor to detonate"); return; }
        useWith(anchor, blank);
        Sync.markCrystal();
        wait = pop ? 0 : delay.get().intValue();
    }

    private void placeAnchor(LivingEntity t, double minD, int r) {
        var p = mc.player;
        int slot = Inv.findHotbar(s -> s.is(Items.RESPAWN_ANCHOR));
        if (slot < 0) {
            if (refill.get() && Inv.refill(s -> s.is(Items.RESPAWN_ANCHOR))) { wait = 2; return; }
            Debug.say(name, debug.get(), "no respawn anchor in hotbar");
            return;
        }
        BlockPos best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        BlockPos tp = t.blockPosition();
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = 0; dy <= 2; dy++)
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = tp.offset(dx, dy, dz);
                    if (!mc.level.isEmptyBlock(pos) || !hasSupport(pos)) continue;
                    Vec3 c = Vec3.atCenterOf(pos);
                    if (p.getEyePosition().distanceTo(c) > reach()) continue;
                    double dt = Dmg.explosion(c, t, 5), ds = Dmg.explosion(c, p, 5);
                    if (dt < minD || !safe(ds)) continue;
                    if (dt - ds * 0.5 > bestScore) { bestScore = dt - ds * 0.5; best = pos; }
                }
        if (best == null) {
            Debug.say(name, debug.get(), "no anchor spot with enough damage (min " + minD + ", max self " + maxSelf.get() + ")");
            return;
        }
        int prev = Inv.selected();
        Inv.select(slot);
        boolean ok = Place.block(best, InteractionHand.MAIN_HAND);
        Inv.select(prev);
        if (ok) wait = delay.get().intValue();
    }

    @Override
    public void onDisable() { wait = 0; }
}