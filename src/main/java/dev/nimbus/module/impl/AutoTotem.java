package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import dev.nimbus.util.Inv;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class AutoTotem extends Module {
    private final NumberSetting health = add(new NumberSetting("Health", 20, 2, 20, 1));
    private final NumberSetting delay = add(new NumberSetting("Delay ticks", 1, 0, 10, 1));
    private int wait;

    public AutoTotem() { super("AutoTotem", "Player", GLFW.GLFW_KEY_X); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.gameMode == null) return;
        if (wait > 0) { wait--; return; }
        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
        if (p.getHealth() + p.getAbsorptionAmount() > health.get()) return;

        int slot = Inv.findInv(s -> s.is(Items.TOTEM_OF_UNDYING));
        if (slot < 0) return;
        Inv.swapToOffhand(Inv.menuSlot(slot));
        wait = delay.get().intValue();
    }
}