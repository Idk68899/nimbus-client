package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class ElytraFly extends Module {
    private final ModeSetting mode = add("General", new ModeSetting("Mode", "Control", "Control", "Boost"));
    private final NumberSetting speed = add("Control", new NumberSetting("Speed", 1.8, 0.5, 5.0, 0.1), () -> mode.get().equals("Control"));
    private final NumberSetting vspeed = add("Control", new NumberSetting("Vertical speed", 0.6, 0.1, 2.0, 0.1), () -> mode.get().equals("Control"));
    private final BoolSetting hover = add("Control", new BoolSetting("Hover when idle", true), () -> mode.get().equals("Control"));
    private final NumberSetting boost = add("Boost", new NumberSetting("Boost per tick", 0.05, 0.01, 0.3, 0.01), () -> mode.get().equals("Boost"));

    public ElytraFly() { super("ElytraFly", "Movement", GLFW.GLFW_KEY_L, "Controls your elytra glide: steer by looking, hold forward to move."); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (!p.isFallFlying()) return;
        var o = mc.options;
        Vec3 look = p.getLookAngle();
        Vec3 v = p.getDeltaMovement();

        if (mode.get().equals("Boost")) {
            if (o.keyUp.isDown()) p.setDeltaMovement(v.add(look.scale(boost.get())));
            return;
        }

        double y = 0;
        if (o.keyJump.isDown()) y += vspeed.get();
        if (o.keyShift.isDown()) y -= vspeed.get(); // CHECK: keyShift
        if (o.keyUp.isDown()) {
            Vec3 d = look.scale(speed.get());
            p.setDeltaMovement(d.x, d.y + y, d.z);
        } else if (hover.get()) {
            p.setDeltaMovement(0, y, 0);
        }
    }
}