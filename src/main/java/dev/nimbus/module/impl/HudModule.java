package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.BoolSetting;
import dev.nimbus.setting.ModeSetting;

public class HudModule extends Module {
    public final BoolSetting arraylist = add(new BoolSetting("Arraylist", true));
    public final BoolSetting watermark = add(new BoolSetting("Watermark", true));
    public final BoolSetting coords = add(new BoolSetting("Coordinates", true));
    public final ModeSetting sort = add(new ModeSetting("Sort", "Length", "Length", "Alphabetical"));

    public HudModule() {
        super("HUD", "Render", -1);
        setEnabled(true);
    }
}