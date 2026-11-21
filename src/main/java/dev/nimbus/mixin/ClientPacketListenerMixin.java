package dev.nimbus.mixin;

import dev.nimbus.module.ModuleManager;
import dev.nimbus.module.impl.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    // CHECK: method name handleSetEntityMotion and packet.id() accessor
    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true)
    private void nimbus$velocity(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        var p = Minecraft.getInstance().player;
        Velocity v = ModuleManager.INSTANCE.get(Velocity.class);
        if (p != null && v != null && v.isEnabled() && packet.id() == p.getId() && v.shouldCancel()) ci.cancel();
    }
}