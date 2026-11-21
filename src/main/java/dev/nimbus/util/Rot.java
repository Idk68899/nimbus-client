package dev.nimbus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
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

    /** Turns toward t by at most maxStep degrees this tick, with jitter and mouse-GCD snapping. */
    public static void smooth(Vec3 t, float maxStep) {
        var p = mc.player;
        float[] r = to(t);
        double s = mc.options.sensitivity().get() * 0.6 + 0.2; // CHECK: sensitivity()
        double gcd = s * s * s * 8.0 * 0.15;

        double dy = Mth.wrapDegrees(r[0] - p.getYRot()) + Rand.between(-0.25, 0.25);
        double dp = (r[1] - p.getXRot()) + Rand.between(-0.25, 0.25);
        dy = Mth.clamp(dy, -maxStep, maxStep);
        dp = Mth.clamp(dp, -maxStep, maxStep);
        dy = Math.round(dy / gcd) * gcd;
        dp = Math.round(dp / gcd) * gcd;

        p.setYRot((float) (p.getYRot() + dy));
        p.setXRot(Mth.clamp((float) (p.getXRot() + dp), -90f, 90f));
    }
}