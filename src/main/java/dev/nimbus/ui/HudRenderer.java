package dev.nimbus.ui;

import dev.nimbus.module.Module;
import dev.nimbus.module.ModuleManager;
import dev.nimbus.module.impl.HudModule;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import java.util.Comparator;
import java.util.List;

public final class HudRenderer {
    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("nimbus", "hud"), HudRenderer::extract);
    }

    private static void extract(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        HudModule hud = ModuleManager.INSTANCE.get(HudModule.class);
        if (hud == null || !hud.isEnabled() || mc.player == null) return;

        var font = mc.font;
        int sw = g.guiWidth();

        if (hud.watermark.get()) {
            g.text(font, "Nimbus 1.0", 3, 3, 0xFF6FA8FF, true);
        }
        if (hud.coords.get()) {
            var p = mc.player;
            g.text(font, String.format("XYZ %.1f %.1f %.1f", p.getX(), p.getY(), p.getZ()), 3, 14, 0xFFFFFFFF, true);
        }
        if (!hud.arraylist.get()) return;

        Comparator<Module> cmp = hud.sort.get().equals("Alphabetical")
                ? Comparator.comparing(m -> m.name)
                : Comparator.comparingInt((Module m) -> font.width(m.name)).reversed();
        List<Module> on = ModuleManager.INSTANCE.all().stream().filter(Module::isEnabled).sorted(cmp).toList();

        float time = (System.currentTimeMillis() % 4000) / 4000f;
        int y = 2;
        for (int i = 0; i < on.size(); i++) {
            String n = on.get(i).name;
            int tw = font.width(n);
            int x = sw - tw - 5;
            int color = rgb((time + i * 0.04f) % 1f, 0.55f, 1f);
            g.fill(x - 2, y, sw, y + 11, 0x90000000);
            g.fill(sw - 1, y, sw, y + 11, color);
            g.text(font, n, x, y + 2, color, true);
            y += 11;
        }
    }

    private static int rgb(float h, float s, float v) {
        int i = (int) (h * 6);
        float f = h * 6 - i, p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float r, gr, b;
        switch (i % 6) {
            case 0 -> { r = v; gr = t; b = p; }
            case 1 -> { r = q; gr = v; b = p; }
            case 2 -> { r = p; gr = v; b = t; }
            case 3 -> { r = p; gr = q; b = v; }
            case 4 -> { r = t; gr = p; b = v; }
            default -> { r = v; gr = p; b = q; }
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (gr * 255) << 8) | (int) (b * 255);
    }
}