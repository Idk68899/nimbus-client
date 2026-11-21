package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import dev.nimbus.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class AutoFirework extends Module {
    private final NumberSetting minSpeed = add("General", new NumberSetting("Boost below speed", 0.6, 0.2, 1.5, 0.05));
    private final NumberSetting cooldown = add("General", new NumberSetting("Cooldown ticks", 25, 5, 80, 5));
    private int wait;

    public AutoFirework() { super("AutoFirework", "Player", -1, "Fires a rocket when your elytra glide slows down."); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.gameMode == null || !p.isFallFlying()) return;
        if (wait > 0) { wait--; return; }
        if (p.getDeltaMovement().length() >= minSpeed.get()) return;
        if (Sync.lockedByOther(this)) return;

        if (p.getOffhandItem().is(Items.FIREWORK_ROCKET)) {
            mc.gameMode.useItem(p, InteractionHand.OFF_HAND);
            wait = cooldown.get().intValue();
            return;
        }
        int slot = Inv.findHotbar(s -> s.is(Items.FIREWORK_ROCKET));
        if (slot < 0) return;
        int prev = Inv.selected();
        Inv.select(slot);
        mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
        Inv.select(prev);
        wait = cooldown.get().intValue();
    }
}