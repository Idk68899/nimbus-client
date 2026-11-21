package dev.nimbus.ui;

import dev.nimbus.module.Module;
import dev.nimbus.module.ModuleManager;
import dev.nimbus.setting.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;          // CHECK: package
import net.minecraft.client.input.MouseButtonEvent; // CHECK: package
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public class ClickGuiScreen extends Screen {
    private static final String[] CATS = { "Combat", "Player", "Movement", "Render" };
    private static final int[] COLORS = { 0xFFE05A5A, 0xFF5AE08A, 0xFF5A9BE0, 0xFFB05AE0 };
    private static final int W = 120, HEAD = 18, ROW = 16, SET = 14;

    private static final class Panel {
        final String cat;
        final int color;
        int x, y, scroll, content;
        boolean open = true;
        Panel(String cat, int color, int x, int y) { this.cat = cat; this.color = color; this.x = x; this.y = y; }
    }

    private static final List<Panel> PANELS = new ArrayList<>();
    static {
        for (int i = 0; i < CATS.length; i++) PANELS.add(new Panel(CATS[i], COLORS[i], 10 + i * (W + 8), 12));
    }

    private interface Click { void on(double mx, double my, int button); }
    private interface Drag { void on(double mx); }
    private record Hit(int x, int y, int w, int h, Click click, Drag drag) {}

    private final List<Hit> hits = new ArrayList<>();
    private final Set<Module> expanded = new HashSet<>();
    private Module listening;
    private Drag activeDrag;
    private Panel dragPanel;
    private int dragDx, dragDy;

    public ClickGuiScreen() { super(Component.literal("Nimbus")); }

    private static String keyName(int k) {
        if (k < 0) return "None";
        return switch (k) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RAlt";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LAlt";
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            default -> {
                String s = GLFW.glfwGetKeyName(k, 0);
                yield s != null ? s.toUpperCase() : "K" + k;
            }
        };
    }

    private int rightX(int x, String s) { return x + W - this.font.width(s) - 5; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        hits.clear();

        for (Panel pn : PANELS) {
            final int x = pn.x, y = pn.y;
            List<Module> mods = ModuleManager.INSTANCE.byCategory(pn.cat);
            long on = mods.stream().filter(Module::isEnabled).count();

            g.fill(x, y, x + W, y + HEAD, 0xF0101018);
            g.fill(x, y, x + W, y + 2, pn.color);
            g.text(this.font, pn.cat.toUpperCase(), x + 6, y + 6, pn.color, true);
            String count = on + "/" + mods.size();
            g.text(this.font, count, rightX(x, count), y + 6, 0xFF888899, false);
            hits.add(new Hit(x, y, W, HEAD, (mx, my, b) -> {
                if (b == 0) { dragPanel = pn; dragDx = (int) (mx - pn.x); dragDy = (int) (my - pn.y); }
                else if (b == 1) pn.open = !pn.open;
            }, null));

            if (!pn.open) continue;

            final int top = y + HEAD;
            int minScroll = Math.min(0, this.height - 30 - top - pn.content);
            if (pn.scroll < minScroll) pn.scroll = minScroll;
            if (pn.scroll > 0) pn.scroll = 0;

            g.enableScissor(x, top, x + W, this.height);
            int cy = top + pn.scroll;

            for (Module m : mods) {
                final int ry = cy;
                boolean visible = ry + ROW > top && ry < this.height;
                boolean hov = visible && mouseX >= x && mouseX < x + W && mouseY >= Math.max(ry, top) && mouseY < ry + ROW;
                if (visible) {
                    g.fill(x, ry, x + W, ry + ROW, hov ? 0xE01E1E2A : 0xD0141420);
                    if (m.isEnabled()) g.fill(x, ry, x + 3, ry + ROW, pn.color);
                    g.text(this.font, m.name, x + 8, ry + 4, m.isEnabled() ? pn.color : 0xFFDDDDDD, true);
                    String tag = m.settings.isEmpty() ? "" : (expanded.contains(m) ? "-" : "+");
                    if (!tag.isEmpty()) g.text(this.font, tag, rightX(x, tag), ry + 4, 0xFF888899, false);
                    if (m.key >= 0) {
                        String kn = keyName(m.key);
                        g.text(this.font, kn, rightX(x, kn) - 12, ry + 4, 0xFF666677, false);
                    }
                    hits.add(new Hit(x, Math.max(ry, top), W, ROW - Math.max(0, top - ry), (mx, my, b) -> {
                        if (b == 0) m.toggle();
                        else if (b == 1) { if (!expanded.remove(m)) expanded.add(m); }
                        else if (b == 2) listening = m;
                    }, null));
                }
                cy += ROW;
                if (!expanded.contains(m)) continue;

                // bind row
                final int by = cy;
                if (by + SET > top && by < this.height) {
                    g.fill(x, by, x + W, by + SET, 0xD00C0C14);
                    g.fill(x, by, x + 1, by + SET, pn.color);
                    g.text(this.font, "Bind", x + 8, by + 3, 0xFFCCCCCC, false);
                    String v = listening == m ? "..." : keyName(m.key);
                    g.text(this.font, v, rightX(x, v), by + 3, pn.color, false);
                    hits.add(new Hit(x, Math.max(by, top), W, SET - Math.max(0, top - by), (mx, my, b) -> {
                        if (b == 0) listening = m;
                        else if (b == 1) m.key = -1;
                    }, null));
                }
                cy += SET;

                for (Setting<?> s : m.settings) {
                    final int sy = cy;
                    if (sy + SET > top && sy < this.height) {
                        g.fill(x, sy, x + W, sy + SET, 0xD00C0C14);
                        g.fill(x, sy, x + 1, sy + SET, pn.color);
                        int hy = Math.max(sy, top), hh = SET - Math.max(0, top - sy);

                        if (s instanceof BoolSetting bs) {
                            g.text(this.font, bs.name, x + 8, sy + 3, 0xFFCCCCCC, false);
                            int bx = x + W - 14;
                            g.fill(bx, sy + 3, bx + 8, sy + 11, 0xFF303040);
                            if (bs.get()) g.fill(bx + 1, sy + 4, bx + 7, sy + 10, pn.color);
                            hits.add(new Hit(x, hy, W, hh, (mx, my, b) -> bs.toggle(), null));
                        } else if (s instanceof ModeSetting ms) {
                            g.text(this.font, ms.name, x + 8, sy + 3, 0xFFCCCCCC, false);
                            String v = ms.get();
                            g.text(this.font, v, rightX(x, v), sy + 3, pn.color, false);
                            hits.add(new Hit(x, hy, W, hh, (mx, my, b) -> ms.cycle(), null));
                        } else if (s instanceof NumberSetting ns) {
                            double frac = (ns.get() - ns.min) / (ns.max - ns.min);
                            g.fill(x + 1, sy, x + 1 + (int) ((W - 1) * frac), sy + SET, (pn.color & 0x00FFFFFF) | 0x66000000);
                            g.text(this.font, ns.name, x + 8, sy + 3, 0xFFFFFFFF, false);
                            String v = ns.step >= 1 ? String.valueOf(ns.get().intValue()) : String.format("%.2f", ns.get());
                            g.text(this.font, v, rightX(x, v), sy + 3, 0xFFFFFFFF, false);
                            Drag d = mx -> ns.set(ns.min + Math.max(0, Math.min(1, (mx - x) / W)) * (ns.max - ns.min));
                            hits.add(new Hit(x, hy, W, hh, (mx, my, b) -> { if (b == 0) d.on(mx); }, d));
                        }
                    }
                    cy += SET;
                }
            }
            g.disableScissor();
            pn.content = cy - (top + pn.scroll);
        }

        g.text(this.font, "LMB toggle  |  RMB settings  |  MMB bind  |  wheel scroll  |  drag headers  |  Esc close",
                10, this.height - 12, 0xFF888899, false);
    }

    // CHECK: signature and event accessors (x(), y(), button())
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (listening != null) { listening = null; return true; }
        double mx = event.x(), my = event.y();
        int b = event.button();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x && mx < h.x + h.w && my >= h.y && my < h.y + h.h) {
                h.click.on(mx, my, b);
                if (h.drag != null && b == 0) activeDrag = h.drag;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    // CHECK: signature
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (activeDrag != null) { activeDrag.on(event.x()); return true; }
        if (dragPanel != null) {
            dragPanel.x = (int) event.x() - dragDx;
            dragPanel.y = (int) event.y() - dragDy;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    // CHECK: signature
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        activeDrag = null;
        dragPanel = null;
        return super.mouseReleased(event);
    }

    // CHECK: signature
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (Panel pn : PANELS) {
            if (mouseX >= pn.x && mouseX < pn.x + W && mouseY >= pn.y) {
                pn.scroll += (int) (scrollY * ROW * 2);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // CHECK: signature (KeyEvent.key())
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listening != null) {
            int k = event.key();
            if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) listening.key = -1;
            else if (k != GLFW.GLFW_KEY_ESCAPE) listening.key = k;
            listening = null;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        ModuleManager.INSTANCE.save();
    }
}