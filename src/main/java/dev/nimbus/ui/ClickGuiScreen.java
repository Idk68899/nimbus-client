package dev.nimbus.ui;

import dev.nimbus.module.Module;
import dev.nimbus.module.ModuleManager;
import dev.nimbus.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;          // CHECK: package
import net.minecraft.client.input.MouseButtonEvent; // CHECK: package
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public class ClickGuiScreen extends Screen {
    private static final String[] CATS = { "Combat", "Player", "Movement", "Render" };
    private static final int W = 112, HEAD = 20, ROW = 17;

    private static final class Panel {
        final String cat;
        int x, y, scroll, max, top, shown, thumbH;
        boolean open = true;
        Panel(String cat) { this.cat = cat; }
    }

    private static final List<Panel> PANELS = new ArrayList<>();
    private static boolean placed;
    static { for (String c : CATS) PANELS.add(new Panel(c)); }

    private interface Click { void on(double mx, double my, int button); }
    private record Hit(int x, int y, int w, int h, Click click) {}

    private final List<Hit> hits = new ArrayList<>();
    private Module listening;
    private Panel dragPanel, dragBar;
    private int dragDx, dragDy, lastMx, lastMy;

    public ClickGuiScreen() { super(Component.literal("Nimbus")); }

    // CHECK: protected void init()
    @Override
    protected void init() {
        if (placed) return;
        placed = true;
        int n = PANELS.size();
        int step = Math.min(W + 6, (this.width - 12 - W) / Math.max(1, n - 1));
        for (int i = 0; i < n; i++) {
            PANELS.get(i).x = 6 + i * step;
            PANELS.get(i).y = 24;
        }
    }

    private static void scrollBy(Panel pn, int d) { pn.scroll = Math.max(0, Math.min(pn.max, pn.scroll + d)); }

    private static void barTo(Panel pn, double my) {
        double span = Math.max(1, pn.shown - pn.thumbH);
        double f = Math.max(0, Math.min(1, (my - pn.top - pn.thumbH / 2.0) / span));
        pn.scroll = (int) Math.round(f * pn.max);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        hits.clear();
        lastMx = mouseX;
        lastMy = mouseY;
        Module hovered = null;

        g.fill(0, 0, this.width, 18, 0xE0101018);
        g.text(this.font, "NIMBUS", 8, 5, 0xFFB05AE0, true);
        String pf = "Profiles";
        int pw = this.font.width(pf) + 12, pbx = this.width - pw - 8;
        boolean pHov = mouseX >= pbx && mouseX < pbx + pw && mouseY >= 3 && mouseY < 15;
        g.fill(pbx, 3, pbx + pw, 15, pHov ? 0xFF3A3A50 : 0xFF2A2A38);
        g.text(this.font, pf, pbx + 6, 5, 0xFFFFFFFF, false);
        hits.add(new Hit(pbx, 3, pw, 12, (mx, my, b) -> Minecraft.getInstance().gui.setScreen(new ProfileScreen(this)))); // CHECK

        for (Panel pn : PANELS) {
            final int x = pn.x, y = pn.y, accent = Theme.color(pn.cat);
            List<Module> mods = ModuleManager.INSTANCE.byCategory(pn.cat);

            int content = mods.size() * ROW;
            int top = y + HEAD;
            int visible = Math.max(ROW, this.height - 22 - top);
            int shown = Math.min(content, visible);
            pn.top = top;
            pn.shown = shown;
            pn.max = Math.max(0, content - visible);
            pn.scroll = Math.max(0, Math.min(pn.max, pn.scroll));

            g.fill(x, y, x + W, y + HEAD, 0xFF16161F);
            g.fill(x, y + HEAD - 2, x + W, y + HEAD, accent);
            g.text(this.font, pn.cat, x + 6, y + 6, 0xFFFFFFFF, true);
            hits.add(new Hit(x, y, W, HEAD, (mx, my, b) -> {
                if (b == 0) { dragPanel = pn; dragDx = (int) (mx - pn.x); dragDy = (int) (my - pn.y); }
                else if (b == 1) pn.open = !pn.open;
            }));
            if (!pn.open) continue;

            g.enableScissor(x, top, x + W, top + shown);
            int cy = top - pn.scroll;
            for (Module m : mods) {
                final int ry = cy;
                int hy = Math.max(ry, top), hb = Math.min(ry + ROW, top + shown);
                if (hb > hy) {
                    boolean hov = mouseX >= x && mouseX < x + W && mouseY >= hy && mouseY < hb;
                    if (hov) hovered = m;
                    g.fill(x, ry, x + W, ry + ROW, m.isEnabled() ? Theme.alpha(accent, 0x40) : hov ? 0xE01B1B26 : 0xE0111119);
                    if (m.isEnabled()) g.fill(x, ry, x + 2, ry + ROW, accent);
                    g.text(this.font, m.name, x + 7, ry + 5, m.isEnabled() ? accent : 0xFFDDDDDD, true);
                    int rightPad = pn.max > 0 ? 8 : 5;
                    if (listening == m) {
                        g.text(this.font, "...", x + W - 14, ry + 5, 0xFFFFFFFF, false);
                    } else if (m.key >= 0) {
                        String kn = Theme.keyName(m.key);
                        g.text(this.font, kn, x + W - this.font.width(kn) - rightPad, ry + 5, 0xFF666677, false);
                    }
                    hits.add(new Hit(x, hy, W, hb - hy, (mx, my, b) -> {
                        if (b == 0) m.toggle();
                        else if (b == 1) Minecraft.getInstance().gui.setScreen(new ModuleScreen(m, this)); // CHECK
                        else if (b == 2) listening = m;
                    }));
                }
                cy += ROW;
            }
            g.disableScissor();

            if (pn.max > 0) {
                int thumbH = Math.max(12, shown * shown / content);
                pn.thumbH = thumbH;
                int thumbY = top + (int) ((shown - thumbH) * (pn.scroll / (double) pn.max));
                g.fill(x + W - 3, top, x + W, top + shown, 0xFF1A1A24);
                g.fill(x + W - 3, thumbY, x + W, thumbY + thumbH, accent);
                hits.add(new Hit(x + W - 7, top, 7, shown, (mx, my, b) -> {
                    if (b == 0) { dragBar = pn; barTo(pn, my); }
                }));
            }
        }

        String tip = hovered != null ? hovered.description : "";
        g.text(this.font, tip.isEmpty()
                        ? "LMB toggle  |  RMB settings  |  MMB bind  |  wheel / arrows / PgUp PgDn scroll  |  drag headers"
                        : tip,
                8, this.height - 12, tip.isEmpty() ? 0xFF888899 : 0xFFDDDDDD, false);
    }

    // CHECK: signature and event accessors (x(), y(), button())
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (listening != null) { listening = null; return true; }
        double mx = event.x(), my = event.y();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x && mx < h.x + h.w && my >= h.y && my < h.y + h.h) {
                h.click.on(mx, my, event.button());
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    // CHECK: signature
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragBar != null) { barTo(dragBar, event.y()); return true; }
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
        dragPanel = null;
        dragBar = null;
        return super.mouseReleased(event);
    }

    // CHECK: signature
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (Panel pn : PANELS) {
            if (mouseX >= pn.x && mouseX < pn.x + W && mouseY >= pn.y) {
                scrollBy(pn, -(int) (scrollY * ROW * 2));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // CHECK: KeyEvent.key()
    @Override
    public boolean keyPressed(KeyEvent event) {
        int k = event.key();
        if (listening != null) {
            if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) listening.key = -1;
            else if (k != GLFW.GLFW_KEY_ESCAPE) listening.key = k;
            listening = null;
            return true;
        }
        int d = switch (k) {
            case GLFW.GLFW_KEY_UP -> -ROW;
            case GLFW.GLFW_KEY_DOWN -> ROW;
            case GLFW.GLFW_KEY_PAGE_UP -> -ROW * 6;
            case GLFW.GLFW_KEY_PAGE_DOWN -> ROW * 6;
            default -> 0;
        };
        if (d != 0) {
            for (Panel pn : PANELS) {
                if (lastMx >= pn.x && lastMx < pn.x + W) { scrollBy(pn, d); return true; }
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() { ModuleManager.INSTANCE.save(); }
}