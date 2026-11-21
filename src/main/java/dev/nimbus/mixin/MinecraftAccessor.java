package dev.nimbus.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    // CHECK: field name rightClickDelay
    @Accessor("rightClickDelay")
    void nimbus$setRightClickDelay(int value);
}