package dev.nimbus.module;

import dev.nimbus.setting.Setting;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final Minecraft mc = Minecraft.getInstance();

    public final String name;
    public final String category;
    public int key;
    public final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;

    protected Module(String name, String category, int defaultKey) {
        this.name = name;
        this.category = category;
        this.key = defaultKey;
    }

    protected <T extends Setting<?>> T add(T s) { settings.add(s); return s; }

    public boolean isEnabled() { return enabled; }
    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean e) {
        if (e == enabled) return;
        enabled = e;
        if (e) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
}