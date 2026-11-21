package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.util.Inv;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoTool extends Module {
    public AutoTool() { super("AutoTool", "Player", -1); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gui.screen() != null) return; // CHECK
        if (!mc.options.keyAttack.isDown()) return;
        if (!(mc.hitResult instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) return;

        var state = mc.level.getBlockState(bhr.getBlockPos());
        int best = Inv.selected();
        float bestSpeed = p.getInventory().getItem(best).getDestroySpeed(state);
        for (int i = 0; i < 9; i++) {
            float s = p.getInventory().getItem(i).getDestroySpeed(state);
            if (s > bestSpeed) { bestSpeed = s; best = i; }
        }
        if (best != Inv.selected()) Inv.select(best);
    }
}