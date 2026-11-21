package dev.nimbus.ui;

import dev.nimbus.module.Module;
import dev.nimbus.module.ModuleManager;
import dev.nimbus.setting.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent; // CHECK: package
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.BiConsumer;

public class ClickGuiScreen extends Screen {
    private static final String[] CATS = { "Combat", "Player", "Movement", "Render" };
    private static final int W = 112, H = 14;

    private record Hit(int x, int y, int w, int h, BiConsumer<Double, Integer> action) {}

    private final List<Hit> hits = new ArrayList<>();
    private final Set<Module> expanded = new HashSet<>();

    public ClickGuiScreen() { super(Component.literal("Nimbus")); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        hits.clear();
        int x = 10;
        for (String cat : CATS) {
            int y = 20;
            g.fill(x, y, x + W, y + H, 0xFF14141F);
            g.text(this.font, cat, x + 4, y + 3, 0xFFFFFFFF, true);
            y += H;
            for (Module m : ModuleManager.INSTANCE.byCategory(cat)) {
                g.fill(x, y, x + W, y + H, m.isEnabled() ? 0xFF3B6FD4 : 0xFF262636);
                g.text(this.font, m.name, x + 4, y + 3, 0xFFFFFFFF, true);
                hits.add(new Hit(x, y, W, H, (mx, b) -> {
                    if (b == 0) m.toggle();
                    else if (b == 1 && !expanded.remove(m)) expanded.add(m);
                }));
                y += H;
                if (!expanded.contains(m)) continue;

                for (Setting<?> s : m.settings) {
                    final int sx = x, sy = y;
                    g.fill(sx, sy, sx + W, sy + H, 0xFF1E1E2C);
                    if (s instanceof BoolSetting bs) {
                        g.text(this.font, bs.name + ": " + (bs.get() ? "on" : "off"), sx + 8, sy + 3, 0xFFCCCCCC, false);
                        hits.add(new Hit(sx, sy, W, H, (mx, b) -> bs.toggle()));
                    } else if (s instanceof ModeSetting ms) {
                        g.text(this.font, ms.name + ": " + ms.get(), sx + 8, sy + 3, 0xFFCCCCCC, false);
                        hits.add(new Hit(sx, sy, W, H, (mx, b) -> ms.cycle()));
                    } else if (s instanceof NumberSetting ns) {
                        double frac = (ns.get() - ns.min) / (ns.max - ns.min);
                        g.fill(sx, sy, sx + (int) (W * frac), sy + H, 0xFF2D4F96);
                        g.text(this.font, ns.name + ": " + String.format("%.2f", ns.get()), sx + 8, sy + 3, 0xFFFFFFFF, false);
                        hits.add(new Hit(sx, sy, W, H, (mx, b) -> ns.set(ns.min + (mx - sx) / W * (ns.max - ns.min))));
                    }
                    y += H;
                }
            }
            x += W + 10;
        }
    }

    // CHECK: signature and event accessors (x(), y(), button())
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (Hit h : hits) {
            if (event.x() >= h.x && event.x() < h.x + h.w && event.y() >= h.y && event.y() < h.y + h.h) {
                h.action.accept(event.x(), event.button());
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void removed() {
        ModuleManager.INSTANCE.save();
    }
}