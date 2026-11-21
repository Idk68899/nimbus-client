package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import dev.nimbus.util.Inv;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class AutoRefill extends Module {
    private record Entry(BoolSetting enabled, Predicate<ItemStack> match) {}

    private final NumberSetting delay = add("General", new NumberSetting("Delay ticks", 2, 0, 10, 1));
    private final List<Entry> entries = new ArrayList<>();
    private int wait;

    public AutoRefill() {
        super("AutoRefill", "Player", -1, "Keeps your hotbar stocked: moves items from your inventory into empty hotbar slots.");
        item("Crystals", true, s -> s.is(Items.END_CRYSTAL));
        item("Obsidian", true, s -> s.is(Items.OBSIDIAN));
        item("Golden apples", true, s -> s.is(Items.GOLDEN_APPLE) || s.is(Items.ENCHANTED_GOLDEN_APPLE));
        item("Healing pots", true, AutoPot::isHealing);
        item("Wind charges", false, s -> s.is(Items.WIND_CHARGE));
        item("Fireworks", false, s -> s.is(Items.FIREWORK_ROCKET));
        item("Ender pearls", false, s -> s.is(Items.ENDER_PEARL));
        item("XP bottles", false, s -> s.is(Items.EXPERIENCE_BOTTLE));
        item("Glowstone", false, s -> s.is(Items.GLOWSTONE));
        item("Respawn anchors", false, s -> s.is(Items.RESPAWN_ANCHOR));
    }

    private void item(String name, boolean def, Predicate<ItemStack> match) {
        entries.add(new Entry(add("Items", new BoolSetting(name, def)), match));
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.gameMode == null || mc.gui.screen() != null) return; // CHECK
        if (wait > 0) { wait--; return; }
        for (Entry e : entries) {
            if (e.enabled.get() && Inv.refill(e.match)) {
                wait = delay.get().intValue();
                return;
            }
        }
    }
}