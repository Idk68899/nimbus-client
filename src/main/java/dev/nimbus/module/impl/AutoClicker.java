package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import dev.nimbus.util.Rand;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;

public class AutoClicker extends Module {
    private final NumberSetting minCps = add(new NumberSetting("Min CPS", 8, 1, 20, 1));
    private final NumberSetting maxCps = add(new NumberSetting("Max CPS", 12, 1, 20, 1));
    private final BoolSetting respectCooldown = add(new BoolSetting("Respect cooldown", false));
    private double acc;

    public AutoClicker() { super("AutoClicker", "Combat", -1); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.gameMode == null || mc.gui.screen() != null) return; // CHECK
        if (!mc.options.keyAttack.isDown()) { acc = 0; return; }
        double lo = Math.min(minCps.get(), maxCps.get()), hi = Math.max(minCps.get(), maxCps.get());
        acc += Rand.between(lo, hi) / 20.0;
        if (acc < 1) return;
        acc -= 1;
        if (respectCooldown.get() && p.getAttackStrengthScale(0.5f) < 0.9f) return;
        if (mc.hitResult instanceof EntityHitResult hit) mc.gameMode.attack(p, hit.getEntity());
        p.swing(InteractionHand.MAIN_HAND);
    }
}