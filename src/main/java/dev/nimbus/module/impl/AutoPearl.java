package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class AutoPearl extends Module {
    private boolean pending;

    public AutoPearl() { super("AutoPearl", "Player", GLFW.GLFW_KEY_P, "Press to throw an ender pearl from your hotbar where you're looking."); }

    @Override
    public void onEnable() { pending = true; }

    @Override
    public void onTick() {
        var p = mc.player;
        if (!pending || p == null || mc.gameMode == null) return;
        if (Sync.lockedByOther(this)) return;
        pending = false;
        setEnabled(false);

        int slot = Inv.findHotbar(s -> s.is(Items.ENDER_PEARL));
        if (slot < 0) return;
        int prev = Inv.selected();
        Inv.select(slot);
        mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
        Inv.select(prev);
    }

    @Override
    public void onDisable() { pending = false; }
}