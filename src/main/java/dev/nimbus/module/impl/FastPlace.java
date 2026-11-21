package dev.nimbus.module.impl;

import dev.nimbus.mixin.MinecraftAccessor;
import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;

public class FastPlace extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay ticks", 0, 0, 3, 1));

    public FastPlace() { super("FastPlace", "Player", -1); }

    @Override
    public void onTick() {
        ((MinecraftAccessor) (Object) mc).nimbus$setRightClickDelay(delay.get().intValue());
    }
}