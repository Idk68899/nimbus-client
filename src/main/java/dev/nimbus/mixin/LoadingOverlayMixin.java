package dev.nimbus.mixin;

import dev.nimbus.ui.LoadingScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Optional mixin (own config, not required): if the target or signature is wrong it is skipped, not fatal.
@Mixin(targets = "net.minecraft.client.gui.screens.LoadingOverlay")
public class LoadingOverlayMixin {
    // CHECK: extractRenderState(GuiGraphicsExtractor, int, int, float)
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void nimbus$brand(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        LoadingScreen.draw(g);
    }
}