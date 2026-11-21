package dev.nimbus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Target {
    private static final Minecraft mc = Minecraft.getInstance();

    public static Vec3 aim(LivingEntity e) {
        return e.position().add(0, e.getBbHeight() * 0.6, 0);
    }

    /** sort: "Distance", "Health" or "Angle". fov 360 disables the angle filter. */
    public static LivingEntity find(double range, boolean playersOnly, String sort, double fov) {
        var p = mc.player;
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) { // CHECK
            if (!(e instanceof LivingEntity le) || le == p || !le.isAlive()) continue;
            if (playersOnly && !(e instanceof Player)) continue;
            if (p.distanceTo(e) > range) continue;
            Vec3 a = aim(le);
            if (fov < 360 && Rot.yawDiff(a) > fov / 2.0) continue;
            double score = switch (sort) {
                case "Health" -> le.getHealth();
                case "Angle" -> Rot.yawDiff(a);
                default -> p.distanceTo(e);
            };
            if (score < bestScore) { bestScore = score; best = le; }
        }
        return best;
    }
}