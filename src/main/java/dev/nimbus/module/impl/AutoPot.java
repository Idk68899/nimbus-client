package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import dev.nimbus.util.Inv;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public class AutoPot extends Module {
    private final NumberSetting health = add(new NumberSetting("Health", 10, 2, 19, 1));
    private final NumberSetting cooldown = add(new NumberSetting("Cooldown ticks", 10, 2, 40, 1));
    private int stage, prev, wait;

    public AutoPot() { super("AutoPot", "Player", -1); }

    private static boolean isHeal(ItemStack s) {
        if (!s.is(Items.SPLASH_POTION)) return false;
        PotionContents pc = s.get(DataComponents.POTION_CONTENTS); // CHECK
        if (pc == null) return false;
        return pc.potion().map(h -> h.value() == Potions.HEALING.value()
                || h.value() == Potions.STRONG_HEALING.value()).orElse(false);
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.gameMode == null) return;
        if (wait > 0) { wait--; return; }

        switch (stage) {
            case 0 -> {
                if (p.getHealth() > health.get()) return;
                int slot = Inv.findHotbar(AutoPot::isHeal);
                if (slot < 0) return;
                prev = Inv.selected();
                Inv.select(slot);
                p.setXRot(90f);   // look down now, throw next tick so the server has the new pitch
                stage = 1; wait = 1;
            }
            case 1 -> {
                mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                stage = 2; wait = 1;
            }
            case 2 -> {
                Inv.select(prev);
                stage = 0; wait = cooldown.get().intValue();
            }
        }
    }

    @Override
    public void onDisable() { stage = 0; wait = 0; }
}