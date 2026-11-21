package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class Step extends Module {
    private final NumberSetting height = add(new NumberSetting("Height", 1.0, 1.0, 2.5, 0.5));

    public Step() { super("Step", "Movement", -1); }

    @Override
    public void onTick() {
        var a = mc.player.getAttribute(Attributes.STEP_HEIGHT); // CHECK: Attributes.STEP_HEIGHT
        if (a != null) a.setBaseValue(height.get());
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        var a = mc.player.getAttribute(Attributes.STEP_HEIGHT);
        if (a != null) a.setBaseValue(0.6);
    }
}