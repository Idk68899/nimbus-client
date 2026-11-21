package dev.nimbus.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class Dmg {
    /** Rough explosion damage: distance falloff only. power: crystal 6, anchor 5. */
    public static double estimate(Vec3 center, Entity e, double power) {
        double d = center.distanceTo(e.position()) / (power * 2.0);
        if (d > 1) return 0;
        double v = 1 - d;
        return (v * v + v) / 2.0 * 7.0 * power * 2.0 + 1.0;
    }
}