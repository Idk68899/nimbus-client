package dev.nimbus.ui;

import dev.nimbus.module.Module;
import dev.nimbus.module.ModuleManager;
import dev.nimbus.setting.*;
import dev.nimbus.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;          // CHECK: package
import net.minecraft.client.input.MouseButtonEvent; // CHECK: package
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public class ModuleScreen extends Screen {
    private static final int W = 340, HEAD = 46, ROW = 20, GH = 16;

    private interface Click { void on(double mx, double my, int button); }
    private interface Drag { void on(double mx); }
    private record Hit(int x, int y, int w, int h, Click click, Drag drag) {}

    private final Screen parent;
    private final Module m;
    private final List<Hit> hits = new ArrayList<>();
    private final Set<String> collapsed = new HashSet<>();
    private int scroll, content;
    private boolean binding;
    private Drag activeDrag;

    public ModuleScreen(Module m, Screen parent) {
        super(Component.literal(m.name));
        this.m = m;
        this.parent = parent;
    }

    private int button(GuiGraphicsExtractor g, int right, int y, String label, int color, int mx, int my, Runnable r) {
        int w = this.font.width(label) + 12, x = right - w;
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + 14;
        g.fill(x, y, x + w, y + 14, hov ? Theme.alpha(color, 0xFF) : Theme.alpha(color, 0xC0));
        g.text(this.font, label, x + 6, y + 3, 0xFFFFFFFF, false);
        hits.add(new Hit(x, y, w, 14, (a, b, c) -> r.run(), null));
        return x - 4;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        hits.clear();

        final int x = (this.width - W) / 2, top = 22, bottom = this.height - 22, accent = Theme.color(m.category);
        g.fill(x, top, x + W, bottom, 0xF0101018);
        g.fill(x, top, x + W, top + 2, accent);
        g.text(this.font, m.name, x + 10, top + 8, accent, true);
        g.text(this.font, m.description.isEmpty() ? m.category : m.description, x + 10, top + 24, 0xFF888899, false);

        int bx = x + W - 8;
        bx = button(g, bx, top + 8, "Back", 0xFF2A2A38, mouseX, mouseY, this::onClose);
        bx = button(g, bx, top + 8, "Reset", 0xFF2A2A38, mouseX, mouseY, m::resetSettings);
        bx = button(g, bx, top + 8, "Bind: " + (binding ? "..." : Theme.keyName(m.key)), 0xFF2A2A38, mouseX, mouseY, () -> binding = true);
        button(g, bx, top + 8, m.isEnabled() ? "ON" : "OFF", m.isEnabled() ? accent : 0xFF2A2A38, mouseX, mouseY, m::toggle);

        final int ly = top + HEAD;
        int minScroll = Math.min(0, (bottom - ly) - content);
        scroll = Math.max(minScroll, Math.min(0, scroll));

        Map<String, List<Setting<?>>> groups = new LinkedHashMap<>();
        for (Setting<?> s : m.settings) if (s.visible.getAsBoolean()) groups.computeIfAbsent(s.group, k -> new ArrayList<>()).add(s);

        g.enableScissor(x, ly, x + W, bottom);
        int cy = ly + scroll;
        for (var e : groups.entrySet()) {
            final String gname = e.getKey();
            boolean col = collapsed.contains(gname);
            final int gy = cy;
            if (gy + GH > ly && gy < bottom) {
                g.fill(x, gy, x + W, gy + GH, 0xFF181824);
                g.text(this.font, (col ? "+ " : "- ") + gname, x + 10, gy + 4, accent, false);
                hits.add(new Hit(x, Math.max(gy, ly), W, GH, (a, b, c) -> { if (!collapsed.remove(gname)) collapsed.add(gname); }, null));
            }
            cy += GH;
            if (col) continue;

            for (Setting<?> s : e.getValue()) {
                final int sy = cy;
                if (sy + ROW > ly && sy < bottom) {
                    boolean hov = mouseX >= x && mouseX < x + W && mouseY >= Math.max(sy, ly) && mouseY < sy + ROW;
                    g.fill(x, sy, x + W, sy + ROW, hov ? 0xE01B1B26 : 0xE0111119);
                    g.text(this.font, s.name, x + 14, sy + 6, 0xFFDDDDDD, false);
                    int hy = Math.max(sy, ly), hh = ROW - Math.max(0, ly - sy);

                    if (s instanceof BoolSetting bs) {
                        int cbx = x + W - 26;
                        g.fill(cbx, sy + 4, cbx + 12, sy + 16, 0xFF303040);
                        if (bs.get()) g.fill(cbx + 2, sy + 6, cbx + 10, sy + 14, accent);
                        hits.add(new Hit(x, hy, W, hh, (a, b, c) -> { if (c == 2) bs.reset(); else bs.toggle(); }, null));
                    } else if (s instanceof ModeSetting ms) {
                        String v = "<  " + ms.get() + "  >";
                        g.text(this.font, v, x + W - this.font.width(v) - 14, sy + 6, accent, false);
                        hits.add(new Hit(x, hy, W, hh, (a, b, c) -> {
                            if (c == 2) ms.reset();
                            else if (c == 1) ms.prev();
                            else ms.cycle();
                        }, null));
                    } else if (s instanceof NumberSetting ns) {
                        final int tx = x + W - 170, tw = 120;
                        double frac = (ns.get() - ns.min) / (ns.max - ns.min);
                        g.fill(tx, sy + 8, tx + tw, sy + 12, 0xFF303040);
                        g.fill(tx, sy + 8, tx + (int) (tw * frac), sy + 12, accent);
                        int kx = tx + (int) (tw * frac);
                        g.fill(kx - 2, sy + 5, kx + 2, sy + 15, 0xFFFFFFFF);
                        String v = ns.step >= 1 ? String.valueOf(ns.get().intValue()) : String.format("%.2f", ns.get());
                        g.text(this.font, v, x + W - 44, sy + 6, 0xFFFFFFFF, false);
                        Drag d = mx -> ns.set(ns.min + Math.max(0, Math.min(1, (mx - tx) / tw)) * (ns.max - ns.min));
                        hits.add(new Hit(tx - 4, hy, tw + 8, hh, (a, b, c) -> { if (c == 0) d.on(a); else if (c == 2) ns.reset(); }, d));
                    }
                }
                cy += ROW;
            }
        }
        g.disableScissor();
        content = cy - (ly + scroll);

        g.text(this.font, "LMB change  |  RMB previous (modes)  |  MMB reset setting  |  wheel scroll  |  Esc back",
                x, bottom + 6, 0xFF888899, false);
    }

    // CHECK: signature and event accessors
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (binding) { binding = false; return true; }
        double mx = event.x(), my = event.y();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x && mx < h.x + h.w && my >= h.y && my < h.y + h.h) {
                h.click.on(mx, my, event.button());
                if (h.drag != null && event.button() == 0) activeDrag = h.drag;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    // CHECK: signature
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (activeDrag != null) { activeDrag.on(event.x()); return true; }
        return super.mouseDragged(event, dragX, dragY);
    }

    // CHECK: signature
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        activeDrag = null;
        return super.mouseReleased(event);
    }

    // CHECK: signature
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll += (int) (scrollY * ROW * 2);
        return true;
    }

    // CHECK: KeyEvent.key()
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (binding) {
            int k = event.key();
            if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) m.key = -1;
            else if (k != GLFW.GLFW_KEY_ESCAPE) m.key = k;
            binding = false;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() { Minecraft.getInstance().gui.setScreen(parent); } // CHECK

    @Override
    public void removed() { ModuleManager.INSTANCE.save(); }
}