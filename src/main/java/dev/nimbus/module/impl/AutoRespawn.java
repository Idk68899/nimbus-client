package dev.nimbus.module.impl;

import dev.nimbus.module.Module;

public class AutoRespawn extends Module {
    public AutoRespawn() { super("AutoRespawn", "Player", -1); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p != null && p.isDeadOrDying()) p.respawn(); // CHECK: LocalPlayer.respawn()
    }
}