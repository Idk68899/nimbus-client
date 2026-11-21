package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.effect.MobEffectInstance;

public class PotionHud extends Module {
    private final NumberSetting x = add("Layout", new NumberSetting("X", 4, 0, 400, 2));
    private final NumberSetting fromBottom = add("Layout", new NumberSetting("Height from bottom", 50, 20, 250, 2));

    public PotionHud() { super("PotionHUD", "Render", -1, "Lists your active effects and their time left."); }

    @Override
    public void renderHud(GuiGraphicsExtractor g) {
        var p = mc.player;
        if (p == null) return;
        int y = g.guiHeight() - fromBottom.get().intValue();
        for (MobEffectInstance e : p.getActiveEffects()) {
            String name = e.getEffect().value().getDisplayName().getString(); // CHECK
            int secs = e.getDuration() / 20;
            String time = e.getDuration() < 0 ? "inf" : String.format("%d:%02d", secs / 60, secs % 60);
            g.text(mc.font, name + " " + (e.getAmplifier() + 1) + "  " + time, x.get().intValue(), y, 0xFFFFFFFF, true);
            y -= 11;
        }
    }
}