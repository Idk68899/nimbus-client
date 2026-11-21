package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.lwjgl.glfw.GLFW;

public class Fullbright extends Module {
    public Fullbright() { super("Fullbright", "Render", GLFW.GLFW_KEY_B); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (!p.hasEffect(MobEffects.NIGHT_VISION))
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100000, 0, false, false, false));
    }

    @Override
    public void onDisable() {
        if (mc.player != null) mc.player.removeEffect(MobEffects.NIGHT_VISION);
    }
}