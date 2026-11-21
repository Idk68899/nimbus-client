package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.NumberSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Keystrokes extends Module {
    private final NumberSetting x = add("Layout", new NumberSetting("X", 6, 0, 400, 2));
    private final NumberSetting y = add("Layout", new NumberSetting("Y", 120, 0, 300, 2));

    public Keystrokes() { super("Keystrokes", "Render", -1, "On-screen WASD and mouse button display."); }

    private void key(GuiGraphicsExtractor g, int kx, int ky, int w, String label, KeyMapping k) {
        g.fill(kx, ky, kx + w, ky + 18, k.isDown() ? 0xC0B05AE0 : 0xA0101018);
        g.text(mc.font, label, kx + (w - mc.font.width(label)) / 2, ky + 5, 0xFFFFFFFF, true);
    }

    @Override
    public void renderHud(GuiGraphicsExtractor g) {
        if (mc.player == null) return;
        int bx = x.get().intValue(), by = y.get().intValue();
        key(g, bx + 20, by, 18, "W", mc.options.keyUp);
        key(g, bx, by + 20, 18, "A", mc.options.keyLeft);
        key(g, bx + 20, by + 20, 18, "S", mc.options.keyDown);
        key(g, bx + 40, by + 20, 18, "D", mc.options.keyRight);
        key(g, bx, by + 40, 28, "LMB", mc.options.keyAttack);
        key(g, bx + 30, by + 40, 28, "RMB", mc.options.keyUse);
    }
}