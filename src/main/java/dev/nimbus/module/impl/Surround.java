
package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import dev.nimbus.util.*;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public class Surround extends Module {
    private final BoolSetting headSides = add("Shape", new BoolSetting("Head sides", false));
    private final BoolSetting top = add("Shape", new BoolSetting("Top cover", false));
    private final BoolSetting obsidianOnly = add("Blocks", new BoolSetting("Obsidian only", true));
    private final NumberSetting delay = add("Blocks", new NumberSetting("Delay ticks", 1, 0, 6, 1));
    private int wait;

    public Surround() { super("Surround", "Combat", GLFW.GLFW_KEY_O, "Places blocks around your feet to block crystal damage."); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        if (wait > 0) { wait--; return; }
        if (Sync.lockedByOther(this)) return;
        int slot = Build.blockSlot(obsidianOnly.get());
        if (slot < 0) return;
        List<BlockPos> order = Build.cover(p.blockPosition(), true, headSides.get(), top.get());
        if (Build.placeNext(order, slot)) wait = delay.get().intValue();
    }
}