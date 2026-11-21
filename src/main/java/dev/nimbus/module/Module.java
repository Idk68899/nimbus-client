package dev.nimbus.module;

import dev.nimbus.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public abstract class Module {
    protected static final Minecraft mc = Minecraft.getInstance();

    public final String name, category, description;
    public int key;
    public final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;

    protected Module(String name, String category, int defaultKey) { this(name, category, defaultKey, ""); }

    protected Module(String name, String category, int defaultKey, String description) {
        this.name = name;
        this.category = category;
        this.key = defaultKey;
        this.description = description;
    }

    protected <T extends Setting<?>> T add(T s) { settings.add(s); return s; }
    protected <T extends Setting<?>> T add(String group, T s) { s.group = group; return add(s); }
    protected <T extends Setting<?>> T add(String group, T s, BooleanSupplier visible) {
        s.visible = visible;
        return add(group, s);
    }

    public boolean isEnabled() { return enabled; }
    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean e) {
        if (e == enabled) return;
        enabled = e;
        if (e) onEnable(); else onDisable();
    }

    public void resetSettings() { for (Setting<?> s : settings) s.reset(); }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void renderHud(GuiGraphicsExtractor g) {}
}