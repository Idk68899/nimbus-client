package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import dev.nimbus.util.*;
import net.minecraft.world.entity.LivingEntity;

public class AimAssist extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", 4.5, 2.0, 8.0, 0.5));
    private final NumberSetting fov = add(new NumberSetting("FOV", 90, 20, 360, 10));
    private final NumberSetting speed = add(new NumberSetting("Speed", 8, 1, 40, 1));
    private final BoolSetting playersOnly = add(new BoolSetting("Players only", true));
    private final BoolSetting onlyWhileClicking = add(new BoolSetting("Only while clicking", true));

    public AimAssist() { super("AimAssist", "Combat", -1); }

    @Override
    public void onTick() {
        if (mc.gui.screen() != null) return; // CHECK
        if (onlyWhileClicking.get() && !mc.options.keyAttack.isDown()) return;
        LivingEntity t = Target.find(range.get(), playersOnly.get(), "Angle", fov.get());
        if (t != null) Rot.smooth(Target.aim(t), speed.get().floatValue());
    }
}