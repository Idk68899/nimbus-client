package dev.nimbus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class Rot {
    private static final Minecraft mc = Minecraft.getInstance();

    public static float[] to(Vec3 t) {
        Vec3 f = mc.player.getEyePosition();
        double dx = t.x - f.x, dy = t.y - f.y, dz = t.z - f.z;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        return new float[]{yaw, pitch};
    }

    public static float yawDiff(Vec3 t) {
        return Math.abs(Mth.wrapDegrees(to(t)[0] - mc.player.getYRot()));
    }

    public static double smooth(Vec3 t, float maxStep) { return smooth(t, maxStep, 0.5f, 0.15f); }

    /**
     * Eased turn: each tick covers `ease` of the remaining angle, capped at maxStep, snapped to the
     * mouse grid. Returns the remaining angle in degrees.
     */
    public static double smooth(Vec3 t, float maxStep, float ease, float noise) {
        var p = mc.player;
        float[] r = to(t);
        double dy = Mth.wrapDegrees(r[0] - p.getYRot());
        double dp = r[1] - p.getXRot();
        double s = mc.options.sensitivity().get() * 0.6 + 0.2; // CHECK: sensitivity()
        double gcd = s * s * s * 8.0 * 0.15;

        double sy = step(dy, maxStep, ease, noise, gcd);
        double sp = step(dp, maxStep, ease, noise, gcd);
        p.setYRot((float) (p.getYRot() + sy));
        p.setXRot(Mth.clamp((float) (p.getXRot() + sp), -90f, 90f));
        return Math.hypot(Mth.wrapDegrees(r[0] - p.getYRot()), r[1] - p.getXRot());
    }

    private static double step(double d, float max, float ease, float noise, double gcd) {
        if (Math.abs(d) < gcd * 0.5) return 0;
        double v = Mth.clamp(d * ease + Rand.between(-noise, noise), -max, max);
        if (Math.abs(v) > Math.abs(d)) v = d;
        v = Math.round(v / gcd) * gcd;
        if (v == 0) v = Math.signum(d) * gcd;
        return v;
    }

    /** True when the player's real look ray passes through the (expanded) hitbox. */
    public static boolean looking(LivingEntity t, double expand, double reach) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 end = eye.add(mc.player.getLookAngle().scale(reach));
        var box = t.getBoundingBox().inflate(expand);
        return box.contains(eye) || box.clip(eye, end).isPresent();
    }
}