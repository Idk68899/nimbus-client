package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.module.ModuleManager;
import dev.nimbus.setting.BoolSetting;
import org.lwjgl.glfw.GLFW;

public class AutoMace extends Module {
    private final BoolSetting killAura = add("General", new BoolSetting("Also enable KillAura", false));
    private final BoolSetting restore = add("General", new BoolSetting("Turn off with this module", true));

    public AutoMace() { super("AutoMace", "Combat", GLFW.GLFW_KEY_I, "One key: wind-charge launch, dive, mace smash. Needs a mace and wind charges in your hotbar."); }

    private static <T extends Module> void set(Class<T> c, boolean on) {
        T m = ModuleManager.INSTANCE.get(c);
        if (m != null) m.setEnabled(on);
    }

    @Override
    public void onEnable() {
        set(WindBoost.class, true);
        set(MaceSmash.class, true);
        if (killAura.get()) set(KillAura.class, true);
    }

    @Override
    public void onDisable() {
        if (!restore.get()) return;
        set(WindBoost.class, false);
        set(MaceSmash.class, false);
        if (killAura.get()) set(KillAura.class, false);
    }
}