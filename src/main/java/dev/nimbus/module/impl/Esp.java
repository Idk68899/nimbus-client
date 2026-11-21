package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.setting.BoolSetting;
import dev.nimbus.util.Sync;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;   // CHECK
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;   // CHECK
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import java.util.HashSet;
import java.util.Set;

public class Esp extends Module {
    private final BoolSetting players = add("Entities", new BoolSetting("Players", true));
    private final BoolSetting hostile = add("Entities", new BoolSetting("Hostile mobs", false));
    private final BoolSetting passive = add("Entities", new BoolSetting("Passive mobs", false));
    private final BoolSetting items = add("Entities", new BoolSetting("Dropped items", false));
    private final BoolSetting onlyTarget = add("Filter", new BoolSetting("Only current target", false));
    private final Set<Integer> marked = new HashSet<>();

    public Esp() { super("ESP", "Render", GLFW.GLFW_KEY_H, "Makes selected entities glow through walls."); }

    private boolean want(Entity e) {
        if (e == mc.player) return false;
        if (onlyTarget.get()) return e == Sync.target();
        if (e instanceof Player) return players.get();
        if (e instanceof Enemy) return hostile.get();
        if (e instanceof Animal) return passive.get();
        if (e instanceof ItemEntity) return items.get();
        return false;
    }

    @Override
    public void onTick() {
        if (mc.level == null) return;
        for (Entity e : mc.level.entitiesForRendering()) { // CHECK
            if (want(e)) { e.setGlowingTag(true); marked.add(e.getId()); } // CHECK: setGlowingTag
            else if (marked.remove(e.getId())) e.setGlowingTag(false);
        }
    }

    @Override
    public void onDisable() {
        if (mc.level != null)
            for (Entity e : mc.level.entitiesForRendering()) if (marked.contains(e.getId())) e.setGlowingTag(false);
        marked.clear();
    }
}