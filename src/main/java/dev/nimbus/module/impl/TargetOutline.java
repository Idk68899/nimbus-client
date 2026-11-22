package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.BoolSetting;
import dev.nimbus.util.Sync;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class TargetOutline extends Module {
    private final BoolSetting playersOnly = add("General", new BoolSetting("Players only", true));
    private int marked = -1;

    public TargetOutline() {
        super("TargetOutline", "Render", -1, "Makes the entity your auras are targeting glow through walls.");
        setEnabled(true);
    }

    private void unmark() {
        if (marked >= 0 && mc.level != null) {
            Entity e = mc.level.getEntity(marked);
            if (e != null) e.setGlowingTag(false); // CHECK: setGlowingTag
        }
        marked = -1;
    }

    @Override
    public void onTick() {
        if (mc.level == null) return;
        LivingEntity t = Sync.target();
        boolean show = t != null && (!playersOnly.get() || t instanceof Player);
        int want = show ? t.getId() : -1;
        if (marked != want) unmark();
        if (show) {
            t.setGlowingTag(true);
            marked = want;
        }
    }

    @Override
    public void onDisable() { unmark(); }
}