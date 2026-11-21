package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;

public class Hitbox extends Module {
    private final NumberSetting expand = add(new NumberSetting("Expand", 0.15, 0.0, 1.0, 0.05));

    public Hitbox() { super("Hitbox", "Combat", -1); }

    public float expand() { return expand.get().floatValue(); }
}