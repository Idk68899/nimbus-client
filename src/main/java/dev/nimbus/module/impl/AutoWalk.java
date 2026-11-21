package dev.nimbus.module.impl;

import dev.nimbus.module.Module;

public class AutoWalk extends Module {
    public AutoWalk() { super("AutoWalk", "Movement", -1); }

    @Override
    public void onTick() { mc.options.keyUp.setDown(true); }

    @Override
    public void onDisable() { mc.options.keyUp.setDown(false); }
}