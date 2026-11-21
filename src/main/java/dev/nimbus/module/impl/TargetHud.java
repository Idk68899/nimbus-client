package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import dev.nimbus.util.Sync;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;

public class TargetHud extends Module {
    private final NumberSetting offX = add("Layout", new NumberSetting("X offset", 30, -200, 300, 5));
    private final NumberSetting offY = add("Layout", new NumberSetting("Y offset", 30, -150, 200, 5));

    public TargetHud() { super("TargetHUD", "Render", -1, "Health, armor, distance and pops for your current target."); }

    @Override
    public void renderHud(GuiGraphicsExtractor g) {
        LivingEntity t = Sync.target();
        if (t == null || mc.player == null) return;
        int x = g.guiWidth() / 2 + offX.get().intValue(), y = g.guiHeight() / 2 + offY.get().intValue();
        float hp = t.getHealth(), max = Math.max(1f, t.getMaxHealth());
        float frac = Math.max(0f, Math.min(1f, hp / max));

        g.fill(x, y, x + 120, y + 36, 0xB0101018);
        g.fill(x, y, x + 120, y + 2, 0xFFE05A5A);
        g.text(mc.font, t.getName().getString(), x + 5, y + 6, 0xFFFFFFFF, true);
        g.fill(x + 5, y + 18, x + 115, y + 23, 0xFF2A2A38);
        int green = (int) (255 * frac), red = (int) (255 * (1 - frac));
        g.fill(x + 5, y + 18, x + 5 + (int) (110 * frac), y + 23, 0xFF000000 | (red << 16) | (green << 8) | 0x40);
        g.text(mc.font, String.format("%.1f hp  %.1fm  armor %d  pops %d",
                hp, mc.player.distanceTo(t), t.getArmorValue(), Sync.pops(t)), x + 5, y + 26, 0xFFAAAAAA, false);
    }
}