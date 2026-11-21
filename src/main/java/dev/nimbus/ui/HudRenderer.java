package dev.nimbus.ui;

import dev.nimbus.module.Module;
import dev.nimbus.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class HudRenderer {
    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("nimbus", "hud"), HudRenderer::extract);
    }

    private static void extract(GuiGraphicsExtractor g, DeltaTracker dt) {
        if (Minecraft.getInstance().player == null) return;
        for (Module m : ModuleManager.INSTANCE.all()) if (m.isEnabled()) m.renderHud(g);
    }
}