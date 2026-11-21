package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import dev.nimbus.util.Rand;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

public class TriggerBot extends Module {
    private final BoolSetting playersOnly = add("General", new BoolSetting("Players only", true));
    private final NumberSetting jitter = add("General", new NumberSetting("Delay jitter", 2, 0, 6, 1));
    private int wait;

    public TriggerBot() { super("TriggerBot", "Combat", -1, "Attacks whatever your crosshair is on once your cooldown is ready."); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.gameMode == null || mc.gui.screen() != null) return; // CHECK
        if (wait > 0) { wait--; return; }
        if (p.getAttackStrengthScale(0.5f) < 0.95f) return;
        if (!(mc.hitResult instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof LivingEntity le) || !le.isAlive()) return;
        if (playersOnly.get() && !(le instanceof Player)) return;
        if (Teams.ignored(le)) return;

        mc.gameMode.attack(p, le);
        p.swing(InteractionHand.MAIN_HAND);
        wait = (int) Rand.between(0, jitter.get() + 1);
    }
}