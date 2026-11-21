package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import dev.nimbus.util.Rand;

public class Reach extends Module {
    private final NumberSetting dist = add(new NumberSetting("Distance", 3.4, 3.0, 6.0, 0.1));
    private final NumberSetting variance = add(new NumberSetting("Variance", 0.2, 0.0, 1.0, 0.05));

    public Reach() { super("Reach", "Combat", -1); }

    public double distance() { return dist.get() + Rand.between(-variance.get(), 0); }
}