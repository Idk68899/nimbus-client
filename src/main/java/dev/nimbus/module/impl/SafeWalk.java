package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SafeWalk extends Module {
    private final NumberSetting lookahead = add("General", new NumberSetting("Look-ahead", 2.0, 1.0, 4.0, 0.5));
    private boolean forced;

    public SafeWalk() { super("SafeWalk", "Movement", -1, "Sneaks automatically when you're about to walk off an edge."); }

    private void release() {
        if (forced) { mc.options.keyShift.setDown(false); forced = false; }
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gui.screen() != null) { release(); return; } // CHECK
        Vec3 v = p.getDeltaMovement();
        AABB box = p.getBoundingBox().move(v.x * lookahead.get(), -0.6, v.z * lookahead.get());
        boolean edge = p.onGround() && mc.level.noCollision(p, box); // CHECK: noCollision(Entity, AABB)
        if (edge) { mc.options.keyShift.setDown(true); forced = true; }
        else release();
    }

    @Override
    public void onDisable() { release(); }
}