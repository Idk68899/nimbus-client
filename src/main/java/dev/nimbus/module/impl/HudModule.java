package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.module.ModuleManager;
import dev.nimbus.setting.*;
import dev.nimbus.util.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.Comparator;
import java.util.List;

public class HudModule extends Module {
    private final BoolSetting arraylist = add("Arraylist", new BoolSetting("Arraylist", true));
    private final ModeSetting align = add("Arraylist", new ModeSetting("Align", "Right", "Right", "Left"), () -> arraylist.get());
    private final ModeSetting colors = add("Arraylist", new ModeSetting("Colors", "Rainbow", "Rainbow", "Category", "Static"), () -> arraylist.get());
    private final ModeSetting sort = add("Arraylist", new ModeSetting("Sort", "Length", "Length", "Alphabetical"), () -> arraylist.get());
    private final BoolSetting background = add("Arraylist", new BoolSetting("Background", true), () -> arraylist.get());
    private final BoolSetting watermark = add("Info", new BoolSetting("Watermark", true));
    private final BoolSetting coords = add("Info", new BoolSetting("Coordinates", true));
    private final BoolSetting fps = add("Info", new BoolSetting("FPS", false));
    private final BoolSetting ping = add("Info", new BoolSetting("Ping", false));

    public HudModule() {
        super("HUD", "Render", -1, "Watermark, info lines and the enabled-module list.");
        setEnabled(true);
    }

    @Override
    public void renderHud(GuiGraphicsExtractor g) {
        var font = mc.font;
        int sw = g.guiWidth();
        int ly = 3;
        if (watermark.get()) { g.text(font, "Nimbus 1.0", 3, ly, 0xFFB05AE0, true); ly += 11; }
        if (coords.get()) {
            var p = mc.player;
            g.text(font, String.format("XYZ %.1f %.1f %.1f", p.getX(), p.getY(), p.getZ()), 3, ly, 0xFFFFFFFF, true);
            ly += 11;
        }
        if (fps.get()) { g.text(font, "FPS " + mc.getFps(), 3, ly, 0xFFFFFFFF, true); ly += 11; } // CHECK: getFps()
        if (ping.get()) {
            var info = mc.player.connection.getPlayerInfo(mc.player.getUUID()); // CHECK
            if (info != null) g.text(font, "Ping " + info.getLatency(), 3, ly, 0xFFFFFFFF, true);
        }
        if (!arraylist.get()) return;

        Comparator<Module> cmp = sort.get().equals("Alphabetical")
                ? Comparator.comparing((Module m) -> m.name)
                : Comparator.comparingInt((Module m) -> font.width(m.name)).reversed();
        List<Module> on = ModuleManager.INSTANCE.all().stream()
                .filter(m -> m.isEnabled() && m != this).sorted(cmp).toList();

        float time = (System.currentTimeMillis() % 4000) / 4000f;
        boolean right = align.get().equals("Right");
        int y = 2;
        for (int i = 0; i < on.size(); i++) {
            Module m = on.get(i);
            int tw = font.width(m.name);
            int x = right ? sw - tw - 5 : 5;
            int color = switch (colors.get()) {
                case "Category" -> Theme.color(m.category);
                case "Static" -> 0xFFB05AE0;
                default -> rgb((time + i * 0.04f) % 1f, 0.55f, 1f);
            };
            if (background.get()) g.fill(right ? x - 2 : 0, y, right ? sw : x + tw + 2, y + 11, 0x90000000);
            g.fill(right ? sw - 1 : 0, y, right ? sw : 1, y + 11, color);
            g.text(font, m.name, x, y + 2, color, true);
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