package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import net.minecraft.world.phys.Vec3;

public class Spider extends Module {
    private final NumberSetting speed = add("General", new NumberSetting("Climb speed", 0.2, 0.1, 0.5, 0.05));

    public Spider() { super("Spider", "Movement", -1, "Climb walls by moving into them."); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p.horizontalCollision && mc.options.keyUp.isDown()) {
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, speed.get(), v.z);
        }
    }
}