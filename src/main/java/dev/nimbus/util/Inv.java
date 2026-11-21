package dev.nimbus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput; // CHECK: package
import net.minecraft.world.item.ItemStack;
import java.util.function.Predicate;

public final class Inv {
    private static final Minecraft mc = Minecraft.getInstance();

    public static int selected() { return mc.player.getInventory().getSelectedSlot(); }      // CHECK
    public static void select(int slot) { mc.player.getInventory().setSelectedSlot(slot); } // CHECK

    public static int findHotbar(Predicate<ItemStack> f) {
        for (int i = 0; i < 9; i++) if (f.test(mc.player.getInventory().getItem(i))) return i;
        return -1;
    }

    public static int findInv(Predicate<ItemStack> f) {
        for (int i = 0; i < 36; i++) if (f.test(mc.player.getInventory().getItem(i))) return i;
        return -1;
    }

    /** Inventory index (0-35) to the player inventory menu slot id. */
    public static int menuSlot(int invIndex) { return invIndex < 9 ? invIndex + 36 : invIndex; }

    public static void swapToOffhand(int menuSlot) { swap(menuSlot, 40); }

    /** Swaps a menu slot with a hotbar slot (0-8). Menu slot 6 is the chest armor slot. */
    public static void swapHotbar(int menuSlot, int hotbar) { swap(menuSlot, hotbar); }

    private static void swap(int menuSlot, int button) {
        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, menuSlot, button, ContainerInput.SWAP, mc.player);
    }

    /** Shift-click a menu slot (equips armor into an empty armor slot). */
    public static void quickMove(int menuSlot) {
        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, menuSlot, 0, ContainerInput.QUICK_MOVE, mc.player);
    }
}