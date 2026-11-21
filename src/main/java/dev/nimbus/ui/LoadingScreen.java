package dev.nimbus.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class LoadingScreen {
    private static final String TITLE = "N I M B U S";

    public static void draw(GuiGraphicsExtractor g) {
        var font = Minecraft.getInstance().font;
        int w = g.guiWidth(), h = g.guiHeight();
        long t = System.currentTimeMillis();

        g.fill(0, 0, w, h, 0xFF0B0B12);
        g.fill(0, h / 2 - 1, w, h / 2, 0xFF16161F);

        int cx = w / 2, cy = h / 2;
        g.text(font, TITLE, cx - font.width(TITLE) / 2, cy - 24, 0xFFB05AE0, true);
        String sub = "loading" + ".".repeat((int) ((t / 400) % 4));
        g.text(font, sub, cx - font.width("loading...") / 2, cy - 10, 0xFF888899, false);

        int bw = Math.min(160, w - 40), bx = cx - bw / 2, by = cy + 6;
        g.fill(bx, by, bx + bw, by + 4, 0xFF1E1E2A);
        double sweep = (t % 1600) / 1600.0;
        int seg = bw / 4;
        int sx = bx + (int) ((bw + seg) * sweep) - seg;
        int x0 = Math.max(bx, sx), x1 = Math.min(bx + bw, sx + seg);
        if (x1 > x0) g.fill(x0, by, x1, by + 4, 0xFFB05AE0);
    }
}