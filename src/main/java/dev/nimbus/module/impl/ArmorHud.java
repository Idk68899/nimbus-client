package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class ArmorHud extends Module {
    private final NumberSetting fromBottom = add("Layout", new NumberSetting("Height from bottom", 70, 30, 200, 2));
    private static final EquipmentSlot[] SLOTS = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private static final String[] LABELS = { "H", "C", "L", "B" };

    public ArmorHud() { super("ArmorHUD", "Render", -1, "Durability bars for your armor above the hotbar."); }

    @Override
    public void renderHud(GuiGraphicsExtractor g) {
        var p = mc.player;
        if (p == null) return;
        int x = g.guiWidth() / 2 - 44, y = g.guiHeight() - fromBottom.get().intValue();
        for (int i = 0; i < 4; i++) {
            ItemStack s = p.getItemBySlot(SLOTS[i]);
            int bx = x + i * 24;
            g.fill(bx, y, bx + 20, y + 14, 0xA0101018);
            g.text(mc.font, LABELS[i], bx + 7, y + 3, s.isEmpty() ? 0xFF555566 : 0xFFFFFFFF, true);
            if (s.isEmpty() || s.getMaxDamage() <= 0) continue;
            float frac = 1f - (float) s.getDamageValue() / s.getMaxDamage();
            int color = frac > 0.5f ? 0xFF5AE08A : frac > 0.25f ? 0xFFE0C05A : 0xFFE05A5A;
            g.fill(bx + 1, y + 11, bx + 1 + (int) (18 * frac), y + 13, color);
        }
    }
}