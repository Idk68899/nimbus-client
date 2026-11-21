package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import dev.nimbus.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public class SafeAnchor extends Module {
    private final ModeSetting trigger = add("Trigger", new ModeSetting("Trigger", "Hazard", "Hazard", "Always"));
    private final NumberSetting radius = add("Trigger", new NumberSetting("Hazard radius", 7, 3, 10, 1), () -> trigger.get().equals("Hazard"));
    private final BoolSetting sides = add("Shape", new BoolSetting("Feet sides", true));
    private final BoolSetting headSides = add("Shape", new BoolSetting("Head sides", true));
    private final BoolSetting top = add("Shape", new BoolSetting("Top cover", true));
    private final BoolSetting obsidianOnly = add("Blocks", new BoolSetting("Obsidian only", true));
    private final NumberSetting delay = add("Blocks", new NumberSetting("Delay ticks", 1, 0, 6, 1));

    private int wait, scan;
    private boolean hazard;

    public SafeAnchor() { super("SafeAnchor", "Combat", GLFW.GLFW_KEY_U, "Boxes you in with obsidian when anchors or crystals are nearby."); }

    private List<BlockPos> order() { return Build.cover(mc.player.blockPosition(), sides.get(), headSides.get(), top.get()); }

    public boolean covered() { return mc.player != null && mc.level != null && Build.complete(order()); }

    private boolean scanHazard() {
        var p = mc.player;
        int r = radius.get().intValue();
        for (Entity e : mc.level.entitiesForRendering()) // CHECK
            if (e instanceof EndCrystal && p.distanceTo(e) <= r) return true;
        BlockPos o = p.blockPosition();
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    var st = mc.level.getBlockState(o.offset(dx, dy, dz));
                    if (st.is(Blocks.RESPAWN_ANCHOR) && st.getValue(RespawnAnchorBlock.CHARGE) > 0) return true;
                }
        return false;
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        if (wait > 0) { wait--; return; }
        if (trigger.get().equals("Hazard")) {
            if (scan-- <= 0) { hazard = scanHazard(); scan = 4; }
            if (!hazard) return;
        }
        if (Sync.lockedByOther(this)) return;
        int slot = Build.blockSlot(obsidianOnly.get());
        if (slot < 0) return;
        if (Build.placeNext(order(), slot)) wait = delay.get().intValue();
    }
}