package dev.nimbus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class Rot {
    private static final Minecraft mc = Minecraft.getInstance();
    private static double velY, velP;
    private static long lastTick = -100;

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

    public static double smooth(Vec3 t, float maxStep) { return smooth(t, maxStep, 0.5f, 0.15f, 0.55f); }

    public static double smooth(Vec3 t, float maxStep, float ease, float noise) { return smooth(t, maxStep, ease, noise, 0.55f); }

    /**
     * Inertial turn. `ease` is the fraction of the remaining angle wanted per tick (capped at maxStep);
     * `follow` is how fast the turn rate chases that (1 = no inertia, lower = smoother accel/decel).
     * Returns the remaining angle in degrees.
     */
    public static double smooth(Vec3 t, float maxStep, float ease, float noise, float follow) {
        var p = mc.player;
        if (Sync.tick - lastTick > 2) { velY = 0; velP = 0; }
        lastTick = Sync.tick;

        float[] r = to(t);
        double dy = Mth.wrapDegrees(r[0] - p.getYRot());
        double dp = r[1] - p.getXRot();
        double s = mc.options.sensitivity().get() * 0.6 + 0.2; // CHECK: sensitivity()
        double gcd = s * s * s * 8.0 * 0.15;

        double wantY = Mth.clamp(dy * ease + Rand.between(-noise, noise), -maxStep, maxStep);
        double wantP = Mth.clamp(dp * ease + Rand.between(-noise, noise), -maxStep, maxStep);
        velY += (wantY - velY) * follow;
        velP += (wantP - velP) * follow;
        if (Math.abs(velY) > Math.abs(dy)) velY = dy;
        if (Math.abs(velP) > Math.abs(dp)) velP = dp;

        p.setYRot((float) (p.getYRot() + snap(velY, dy, gcd)));
        p.setXRot(Mth.clamp((float) (p.getXRot() + snap(velP, dp, gcd)), -90f, 90f));
        return Math.hypot(Mth.wrapDegrees(r[0] - p.getYRot()), r[1] - p.getXRot());
    }

    private static double snap(double v, double d, double gcd) {
        if (Math.abs(d) < gcd * 0.5) return 0;
        double q = Math.round(v / gcd) * gcd;
        if (q == 0) q = Math.signum(d) * gcd;
        return q;
    }

    /** True when the player's real look ray passes through the (expanded) hitbox. */
    public static boolean looking(LivingEntity t, double expand, double reach) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 end = eye.add(mc.player.getLookAngle().scale(reach));
        var box = t.getBoundingBox().inflate(expand);
        return box.contains(eye) || box.clip(eye, end).isPresent();
    }
}