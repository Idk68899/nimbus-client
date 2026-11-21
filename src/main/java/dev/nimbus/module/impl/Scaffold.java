package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import dev.nimbus.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import org.lwjgl.glfw.GLFW;

public class Scaffold extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay ticks", 1, 0, 6, 1));
    private int wait;

    public Scaffold() { super("Scaffold", "Movement", GLFW.GLFW_KEY_J); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        if (wait > 0) { wait--; return; }

        var pos = p.blockPosition().below();
        if (!mc.level.getBlockState(pos).canBeReplaced()) return;
        int slot = Inv.findHotbar(s -> s.getItem() instanceof BlockItem);
        if (slot < 0) return;
        if (Inv.selected() != slot) Inv.select(slot);
        if (Place.block(pos, InteractionHand.MAIN_HAND)) wait = delay.get().intValue();
    }
}