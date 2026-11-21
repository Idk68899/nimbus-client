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
    private static final int W = 118, HEAD = 20, ROW = 17;

    private static final class Panel {
        final String cat;
        int x, y, scroll, content;
        boolean open = true;
        Panel(String cat, int x, int y) { this.cat = cat; this.x = x; this.y = y; }
    }

    private static final List<Panel> PANELS = new ArrayList<>();
    static {
        for (int i = 0; i < CATS.length; i++) PANELS.add(new Panel(CATS[i], 8 + i * (W + 6), 26));
    }

    private interface Click { void on(double mx, double my, int button); }
    private record Hit(int x, int y, int w, int h, Click click) {}

    private final List<Hit> hits = new ArrayList<>();
    private Module listening;
    private Panel dragPanel;
    private int dragDx, dragDy;

    public ClickGuiScreen() { super(Component.literal("Nimbus")); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        hits.clear();
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

            g.fill(x, y, x + W, y + HEAD, 0xFF16161F);
            g.fill(x, y + HEAD - 2, x + W, y + HEAD, accent);
            g.text(this.font, pn.cat, x + 6, y + 6, 0xFFFFFFFF, true);
            hits.add(new Hit(x, y, W, HEAD, (mx, my, b) -> {
                if (b == 0) { dragPanel = pn; dragDx = (int) (mx - pn.x); dragDy = (int) (my - pn.y); }
                else if (b == 1) pn.open = !pn.open;
            }));
            if (!pn.open) continue;

            final int top = y + HEAD;
            int minScroll = Math.min(0, this.height - 30 - top - pn.content);
            pn.scroll = Math.max(minScroll, Math.min(0, pn.scroll));

            g.enableScissor(x, top, x + W, this.height);
            int cy = top + pn.scroll;
            for (Module m : mods) {
                final int ry = cy;
                if (ry + ROW > top && ry < this.height) {
                    boolean hov = mouseX >= x && mouseX < x + W && mouseY >= Math.max(ry, top) && mouseY < ry + ROW;
                    if (hov) hovered = m;
                    g.fill(x, ry, x + W, ry + ROW, m.isEnabled() ? Theme.alpha(accent, 0x40) : hov ? 0xE01B1B26 : 0xE0111119);
                    if (m.isEnabled()) g.fill(x, ry, x + 2, ry + ROW, accent);
                    g.text(this.font, m.name, x + 7, ry + 5, m.isEnabled() ? accent : 0xFFDDDDDD, true);
                    if (listening == m) {
                        g.text(this.font, "...", x + W - 14, ry + 5, 0xFFFFFFFF, false);
                    } else if (m.key >= 0) {
                        String kn = Theme.keyName(m.key);
                        g.text(this.font, kn, x + W - this.font.width(kn) - 5, ry + 5, 0xFF666677, false);
                    }
                    hits.add(new Hit(x, Math.max(ry, top), W, ROW - Math.max(0, top - ry), (mx, my, b) -> {
                        if (b == 0) m.toggle();
                        else if (b == 1) Minecraft.getInstance().gui.setScreen(new ModuleScreen(m, this)); // CHECK
                        else if (b == 2) listening = m;
                    }));
                }
                cy += ROW;
            }
            g.disableScissor();
            pn.content = cy - (top + pn.scroll);
        }

        String tip = hovered != null ? hovered.description : "";
        g.text(this.font, tip.isEmpty()
                        ? "LMB toggle  |  RMB settings  |  MMB bind  |  wheel scroll  |  drag headers"
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

    // CHECK: KeyEvent.key()
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
    public void removed() { ModuleManager.INSTANCE.save(); }
}