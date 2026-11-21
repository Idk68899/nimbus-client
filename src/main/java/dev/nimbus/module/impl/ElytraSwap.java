package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.util.Inv;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import java.util.function.Predicate;

public class ElytraSwap extends Module {
    private boolean pending;

    public ElytraSwap() { super("ElytraSwap", "Player", GLFW.GLFW_KEY_K, "Press to swap your chest slot between elytra and a chestplate."); }

    @Override
    public void onEnable() { pending = true; }

    @Override
    public void onTick() {
        var p = mc.player;
        if (!pending || p == null || mc.gameMode == null) return;
        pending = false;
        setEnabled(false);

        boolean wearingElytra = p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
        Predicate<ItemStack> want = wearingElytra ? s -> s.is(ItemTags.CHEST_ARMOR) : s -> s.is(Items.ELYTRA);

        int hb = Inv.findHotbar(want);
        if (hb >= 0) {
            Inv.swapHotbar(6, hb);
            return;
        }
        int inv = Inv.findInv(want);
        if (inv < 0) return;
        int sel = Inv.selected();
        Inv.swapHotbar(Inv.menuSlot(inv), sel);   // bring it into the selected hotbar slot
        Inv.swapHotbar(6, sel);                   // then swap that with the chest slot
    }

    @Override
    public void onDisable() { pending = false; }
}