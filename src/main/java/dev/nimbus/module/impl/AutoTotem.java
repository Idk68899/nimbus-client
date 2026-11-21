package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import dev.nimbus.util.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class AutoTotem extends Module {
    private final ModeSetting mode = add("General", new ModeSetting("Mode", "Always", "Always", "Low health"));
    private final NumberSetting health = add("General", new NumberSetting("Health", 14, 2, 20, 1), () -> mode.get().equals("Low health"));
    private final NumberSetting delay = add("General", new NumberSetting("Delay ticks", 0, 0, 10, 1));
    private final BoolSetting popRefill = add("Reaction", new BoolSetting("Instant refill after pop", true));
    private final BoolSetting fall = add("Reaction", new BoolSetting("Fall damage", true));
    private final BoolSetting crystals = add("Reaction", new BoolSetting("Crystal nearby", true));
    private int wait;

    public AutoTotem() { super("AutoTotem", "Player", GLFW.GLFW_KEY_X, "Keeps a totem in your offhand and refills it the tick after a pop."); }

    private boolean crystalNear() {
        for (Entity e : mc.level.entitiesForRendering()) // CHECK
            if (e instanceof EndCrystal && mc.player.distanceTo(e) < 12) return true;
        return false;
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.gameMode == null || mc.level == null) return;
        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) { wait = 0; return; }

        boolean pop = popRefill.get() && Sync.recentPop(p, 5);
        if (!pop && mode.get().equals("Low health")) {
            double hp = Dmg.hp(p);
            boolean danger = hp <= health.get()
                    || (fall.get() && p.fallDistance - 3 >= hp) // CHECK fallDistance
                    || (crystals.get() && crystalNear());
            if (!danger) return;
        }
        if (wait > 0 && !pop) { wait--; return; }

        int slot = Inv.findInv(s -> s.is(Items.TOTEM_OF_UNDYING));
        if (slot < 0) return;
        Inv.swapToOffhand(Inv.menuSlot(slot));
        wait = delay.get().intValue();
    }
}