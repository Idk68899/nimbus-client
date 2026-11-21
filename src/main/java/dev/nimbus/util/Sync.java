package dev.nimbus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.Map;

/** Shared state so modules cooperate instead of fighting over the hotbar and target. */
public final class Sync {
    public static long tick;

    private static LivingEntity target;
    private static long targetTick;
    private static Object owner;
    private static long lockUntil;
    private static long crystalTick = -100;
    private static final Map<Integer, Long> popTick = new HashMap<>();
    private static final Map<Integer, Integer> popCount = new HashMap<>();

    public static boolean lock(Object who, int ticks) {
        if (lockedByOther(who)) return false;
        owner = who;
        lockUntil = tick + ticks;
        return true;
    }
    public static boolean lockedByOther(Object who) { return owner != null && owner != who && tick < lockUntil; }
    public static void unlock(Object who) { if (owner == who) lockUntil = 0; }

    public static void setTarget(LivingEntity e) { target = e; targetTick = tick; }
    public static LivingEntity target() {
        return target != null && tick - targetTick <= 10 && target.isAlive() ? target : null;
    }

    public static void markCrystal() { crystalTick = tick; }
    public static boolean crystalActive() { return tick - crystalTick <= 3; }

    public static void pop(int id) { popTick.put(id, tick); popCount.merge(id, 1, Integer::sum); }
    public static boolean recentPop(Entity e, int within) {
        Long t = popTick.get(e.getId());
        return t != null && tick - t <= within;
    }
    public static int pops(Entity e) { return popCount.getOrDefault(e.getId(), 0); }

    public static boolean hasTotem() {
        var p = Minecraft.getInstance().player;
        return p != null && (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
                || Inv.findInv(s -> s.is(Items.TOTEM_OF_UNDYING)) >= 0);
    }
}