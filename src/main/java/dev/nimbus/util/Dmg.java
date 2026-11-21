package dev.nimbus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Dmg {
    private static final Minecraft mc = Minecraft.getInstance();

    private static double exposure(Vec3 c, Entity e) {
        Vec3[] pts = {
                new Vec3(e.getX(), e.getY() + 0.1, e.getZ()),
                new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ()),
                e.getEyePosition()
        };
        int open = 0;
        for (Vec3 pt : pts) {
            var hit = mc.level.clip(new ClipContext(c, pt, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)); // CHECK ctor
            if (hit.getType() == HitResult.Type.MISS) open++;
        }
        return open / 3.0;
    }

    /** Explosion damage at `c` for radius 6 (crystal) or 5 (anchor), with armor, difficulty and exposure. */
    public static double explosion(Vec3 c, LivingEntity e, double radius) {
        double doubled = radius * 2;
        double dist = c.distanceTo(e.position());
        if (dist >= doubled) return 0;
        double v = (1 - dist / doubled) * exposure(c, e);
        double dmg = (v * v + v) / 2.0 * 7.0 * doubled + 1.0;

        if (e instanceof Player) {
            switch (mc.level.getDifficulty()) { // CHECK
                case PEACEFUL -> { return 0; }
                case EASY -> dmg = Math.min(dmg / 2 + 1, dmg);
                case HARD -> dmg *= 1.5;
                default -> {}
            }
        }

        double armor = e.getArmorValue();
        double tough = e.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        double f = 2 + tough / 4;
        double red = Math.max(armor * 0.2, Math.min(20, armor - dmg / f)) / 25.0;
        dmg *= 1 - red;

        var res = e.getEffect(MobEffects.RESISTANCE); // CHECK: field name
        if (res != null) dmg *= Math.max(0, 1 - 0.2 * (res.getAmplifier() + 1));
        return Math.max(0, dmg);
    }

    public static double hp(LivingEntity e) { return e.getHealth() + e.getAbsorptionAmount(); }
}