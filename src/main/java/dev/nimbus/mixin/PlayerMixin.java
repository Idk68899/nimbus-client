package dev.nimbus.mixin;

import dev.nimbus.module.ModuleManager;
import dev.nimbus.module.impl.Reach;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
    // CHECK: method name entityInteractionRange
    @Inject(method = "entityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void nimbus$reach(CallbackInfoReturnable<Double> cir) {
        if ((Object) this != Minecraft.getInstance().player) return;
        Reach r = ModuleManager.INSTANCE.get(Reach.class);
        if (r != null && r.isEnabled()) cir.setReturnValue(Math.max(cir.getReturnValueD(), r.distance()));
    }
}