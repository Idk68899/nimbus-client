package dev.nimbus.ui;

import dev.nimbus.module.Profiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent; // CHECK: package
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public class ProfileScreen extends Screen {
    private static final int W = 240, ROW = 20;

    private interface Click { void on(int button); }
    private record Hit(int x, int y, int w, int h, Click click) {}

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private String status = "";

    public ProfileScreen(Screen parent) {
        super(Component.literal("Profiles"));
        this.parent = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        hits.clear();
        List<String> names = Profiles.list();
        int x = (this.width - W) / 2, y = 30;
        int height = 30 + (names.size() + 1) * ROW + 24;

        g.fill(x, y - 8, x + W, y - 8 + height, 0xF0101018);
        g.fill(x, y - 8, x + W, y - 6, 0xFFB05AE0);
        g.text(this.font, "Profiles", x + 10, y, 0xFFB05AE0, true);
        g.text(this.font, "saves keybinds, settings and which modules are on", x + 10, y + 12, 0xFF888899, false);
        int ry = y + 28;

        for (String n : names) {
            final String name = n;
            boolean hov = mouseX >= x && mouseX < x + W && mouseY >= ry && mouseY < ry + ROW;
            g.fill(x, ry, x + W, ry + ROW, hov ? 0xE01B1B26 : 0xE0111119);
            g.text(this.font, name, x + 14, ry + 6, 0xFFDDDDDD, false);
            hits.add(new Hit(x, ry, W, ROW, b -> {
                if (b == 0) { Profiles.load(name); status = "Loaded " + name; }
                else if (b == 1) { Profiles.delete(name); status = "Deleted " + name; }
                else if (b == 2) { Profiles.save(name); status = "Overwrote " + name; }
            }));
            ry += ROW;
        }

        boolean hov = mouseX >= x && mouseX < x + W && mouseY >= ry && mouseY < ry + ROW;
        g.fill(x, ry, x + W, ry + ROW, hov ? 0xE02D4F96 : 0xE01E3A70);
        g.text(this.font, "+ Save current as new profile", x + 14, ry + 6, 0xFFFFFFFF, false);
        hits.add(new Hit(x, ry, W, ROW, b -> {
            String n = Profiles.newName();
            Profiles.save(n);
            status = "Saved " + n;
        }));
        ry += ROW + 6;

        g.text(this.font, status.isEmpty() ? "LMB load  |  RMB delete  |  MMB overwrite  |  Esc back" : status,
                x + 10, ry, 0xFF888899, false);
    }

    // CHECK: signature
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        for (Hit h : hits) {
            if (mx >= h.x && mx < h.x + h.w && my >= h.y && my < h.y + h.h) {
                h.click.on(event.button());
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() { Minecraft.getInstance().gui.setScreen(parent); } // CHECK
}