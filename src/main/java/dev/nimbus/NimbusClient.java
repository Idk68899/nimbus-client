package dev.nimbus;

import dev.nimbus.module.ModuleManager;
import dev.nimbus.ui.ClickGuiScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;

public class NimbusClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModuleManager.INSTANCE.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ModuleManager.INSTANCE.tick(client);
            if (client.player != null && client.gui.screen() == null) { // CHECK: gui.screen()
                long h = GLFW.glfwGetCurrentContext();
                if (GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
                    client.gui.setScreen(new ClickGuiScreen());
                }
            }
        });
    }
}