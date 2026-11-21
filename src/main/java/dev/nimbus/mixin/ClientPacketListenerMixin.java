package dev.nimbus.mixin;

import dev.nimbus.module.ModuleManager;
import dev.nimbus.module.impl.Velocity;
import dev.nimbus.util.Sync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Unique private Vec3 nimbus$prev;

    // CHECK: method name handleSetEntityMotion and packet.id() accessor
    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"))
    private void nimbus$velHead(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        var p = Minecraft.getInstance().player;
        nimbus$prev = (p != null && packet.id() == p.getId()) ? p.getDeltaMovement() : null;
    }

    @Inject(method = "handleSetEntityMotion", at = @At("RETURN"))
    private void nimbus$velTail(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        var p = Minecraft.getInstance().player;
        Velocity v = ModuleManager.INSTANCE.get(Velocity.class);
        if (nimbus$prev != null && p != null && v != null && v.isEnabled() && v.shouldApply()) {
            Vec3 d = p.getDeltaMovement().subtract(nimbus$prev);
            p.setDeltaMovement(
                    nimbus$prev.x + d.x * v.horizontal(),
                    nimbus$prev.y + d.y * v.vertical(),
                    nimbus$prev.z + d.z * v.horizontal());
        }
        nimbus$prev = null;
    }

    // CHECK: method name handleEntityEvent, getEventId(), getEntity(Level)
    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void nimbus$pop(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || packet.getEventId() != 35) return;
        Entity e = packet.getEntity(mc.level);
        if (e != null) Sync.pop(e.getId());
    }
}