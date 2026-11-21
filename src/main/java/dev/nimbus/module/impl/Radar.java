package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;   // CHECK
import net.minecraft.world.entity.monster.Enemy;   // CHECK
import net.minecraft.world.entity.player.Player;

public class Radar extends Module {
    private final NumberSetting size = add("Layout", new NumberSetting("Size", 80, 50, 140, 10));
    private final NumberSetting x = add("Layout", new NumberSetting("X", 4, 0, 400, 2));
    private final NumberSetting y = add("Layout", new NumberSetting("Y", 40, 0, 300, 2));
    private final NumberSetting range = add("Radar", new NumberSetting("Range (blocks)", 32, 8, 96, 4));
    private final BoolSetting players = add("Radar", new BoolSetting("Players", true));
    private final BoolSetting hostile = add("Radar", new BoolSetting("Hostile", true));
    private final BoolSetting passive = add("Radar", new BoolSetting("Passive", false));

    public Radar() { super("Radar", "Render", -1, "Top-down minimap of nearby entities."); }

    @Override
    public void renderHud(GuiGraphicsExtractor g) {
        var p = mc.player;
        if (p == null || mc.level == null) return;
        int s = size.get().intValue(), px = x.get().intValue(), py = y.get().intValue();
        int cx = px + s / 2, cy = py + s / 2;
        g.fill(px, py, px + s, py + s, 0xA0101018);
        g.fill(px, py, px + s, py + 1, 0xFFB05AE0);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);

        double yaw = Math.toRadians(p.getYRot());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        double scale = (s / 2.0) / range.get();

        for (Entity e : mc.level.entitiesForRendering()) { // CHECK
            if (e == p || !(e instanceof LivingEntity)) continue;
            int color;
            if (e instanceof Player) { if (!players.get()) continue; color = 0xFFFF5A5A; }
            else if (e instanceof Enemy) { if (!hostile.get()) continue; color = 0xFFFFA040; }
            else if (e instanceof Animal) { if (!passive.get()) continue; color = 0xFF5AE08A; }
            else continue;
            double dx = e.getX() - p.getX(), dz = e.getZ() - p.getZ();
            double sx = (dx * rx + dz * rz) * scale, sy = (dx * fx + dz * fz) * scale;
            if (Math.abs(sx) > s / 2.0 - 2 || Math.abs(sy) > s / 2.0 - 2) continue;
            int ex = (int) (cx + sx), ey = (int) (cy - sy);
            g.fill(ex - 1, ey - 1, ex + 2, ey + 2, color);
        }
    }
}