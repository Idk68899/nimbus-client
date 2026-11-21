package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.BoolSetting;
import dev.nimbus.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class KillAura extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", 3.0, 1.0, 6.0, 0.1));
    private final BoolSetting playersOnly = add(new BoolSetting("Players only", true));
    private final BoolSetting rotate = add(new BoolSetting("Rotate", true));

    public KillAura() { super("KillAura", "Combat", GLFW.GLFW_KEY_R); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        if (p.getAttackStrengthScale(0.5f) < 0.95f) return;

        LivingEntity best = null;
        double bestD = range.get() * range.get();
        for (Entity e : mc.level.entitiesForRendering()) { // CHECK: entitiesForRendering()
            if (!(e instanceof LivingEntity le) || le == p || !le.isAlive()) continue;
            if (playersOnly.get() && !(e instanceof Player)) continue;
            double d = p.distanceToSqr(e);
            if (d <= bestD) { bestD = d; best = le; }
        }
        if (best == null) return;

        if (rotate.get()) {
            Vec3 from = p.getEyePosition(), to = best.getEyePosition();
            double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
            p.setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
            p.setXRot((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        }
        mc.gameMode.attack(p, best);
        p.swing(InteractionHand.MAIN_HAND);
    }
}