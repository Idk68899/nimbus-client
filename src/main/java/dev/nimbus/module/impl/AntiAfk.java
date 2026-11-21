package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.util.Rand;
import net.minecraft.world.phys.Vec3;

public class AntiAfk extends Module {
    private int timer = 100;

    public AntiAfk() { super("AntiAFK", "Player", -1); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || --timer > 0) return;
        timer = (int) Rand.between(100, 300);
        p.setYRot(p.getYRot() + (float) Rand.between(-25, 25));
        if (p.onGround()) {
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, 0.42, v.z);
        }
    }
}