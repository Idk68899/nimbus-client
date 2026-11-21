package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import dev.nimbus.util.Rand;

public class Velocity extends Module {
    private final NumberSetting horizontal = add("General", new NumberSetting("Horizontal %", 0, 0, 100, 5));
    private final NumberSetting vertical = add("General", new NumberSetting("Vertical %", 100, 0, 100, 5));
    private final NumberSetting chance = add("General", new NumberSetting("Chance %", 100, 0, 100, 5));

    public Velocity() { super("Velocity", "Combat", -1, "Scales the knockback you take."); }

    public boolean shouldApply() { return Rand.between(0, 100) < chance.get(); }
    public double horizontal() { return horizontal.get() / 100.0; }
    public double vertical() { return vertical.get() / 100.0; }
}