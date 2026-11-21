package dev.nimbus.mixin;

import dev.nimbus.module.ModuleManager;
import dev.nimbus.module.impl.Hitbox;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    // CHECK: method name getPickRadius
    @Inject(method = "getPickRadius", at = @At("RETURN"), cancellable = true)
    private void nimbus$hitbox(CallbackInfoReturnable<Float> cir) {
        Hitbox h = ModuleManager.INSTANCE.get(Hitbox.class);
        if (h == null || !h.isEnabled()) return;
        Object self = this;
        if (self == Minecraft.getInstance().player || !(self instanceof LivingEntity)) return;
        cir.setReturnValue(cir.getReturnValueF() + h.expand());
    }
}