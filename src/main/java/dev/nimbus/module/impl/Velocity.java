package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import dev.nimbus.util.Rand;

public class Velocity extends Module {
    private final NumberSetting chance = add(new NumberSetting("Chance %", 100, 0, 100, 5));

    public Velocity() { super("Velocity", "Combat", -1); }

    public boolean shouldCancel() { return Rand.between(0, 100) < chance.get(); }
}