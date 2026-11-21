package dev.nimbus.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class Gear {
    public static boolean isMace(ItemStack s) { return s.is(Items.MACE); }

    /** Spears are matched by item id (wooden_spear, iron_spear, ...), so no tag lookup is needed. */
    public static boolean isSpear(ItemStack s) {
        return !s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().endsWith("_spear"); // CHECK: getPath()
    }
}