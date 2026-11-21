package dev.nimbus.module.impl;

import dev.nimbus.module.Module;
import dev.nimbus.module.ModuleManager;
import dev.nimbus.setting.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor; // CHECK: package
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public class Teams extends Module {
    private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    private final BoolSetting scoreboard = add("Detection", new BoolSetting("Scoreboard team", true));
    private final BoolSetting nameColor = add("Detection", new BoolSetting("Name color (off if everyone shares a rank color)", false));
    private final BoolSetting armorColor = add("Detection", new BoolSetting("Dyed armor color", false));
    private final BoolSetting friends = add("Friends", new BoolSetting("Use friends list", true));
    private final BoolSetting middleClick = add("Friends", new BoolSetting("Middle-click to add/remove", true), () -> friends.get());
    private final StringSetting list = add("Friends", new StringSetting("List", ""));
    private boolean mmbDown;

    public Teams() {
        super("Teams", "Player", -1, "Combat modules skip teammates and friends. Middle-click a player to add or remove a friend.");
        setEnabled(true);
    }

    /** True if combat modules should leave this entity alone. */
    public static boolean ignored(Entity e) {
        Teams t = ModuleManager.INSTANCE.get(Teams.class);
        return t != null && t.isEnabled() && t.check(e);
    }

    private boolean check(Entity e) {
        var me = mc.player;
        if (me == null || !(e instanceof Player pl) || pl == me) return false;
        if (friends.get() && names().contains(pl.getName().getString().toLowerCase())) return true;
        if (scoreboard.get() && me.isAlliedTo(pl)) return true; // CHECK: isAlliedTo
        if (nameColor.get()) {
            Integer a = color(me), b = color(pl);
            if (a != null && a.equals(b)) return true;
        }
        if (armorColor.get()) {
            Set<Integer> mine = dyes(me);
            if (!mine.isEmpty() && !Collections.disjoint(mine, dyes(pl))) return true;
        }
        return false;
    }

    private static Integer color(Entity e) {
        Component n = e.getDisplayName();
        TextColor c = n.getStyle().getColor();
        if (c == null) {
            for (Component s : n.getSiblings()) {
                c = s.getStyle().getColor();
                if (c != null) break;
            }
        }
        return c == null ? null : c.getValue();
    }

    private static Set<Integer> dyes(LivingEntity e) {
        Set<Integer> out = new HashSet<>();
        for (EquipmentSlot s : ARMOR) {
            ItemStack st = e.getItemBySlot(s);
            DyedItemColor d = st.get(DataComponents.DYED_COLOR); // CHECK: DYED_COLOR / rgb()
            if (d != null) out.add(d.rgb());
        }
        return out;
    }

    private Set<String> names() {
        Set<String> s = new HashSet<>();
        for (String n : list.get().split(",")) if (!n.isBlank()) s.add(n.trim().toLowerCase());
        return s;
    }

    private void toggleFriend(Player pl) {
        String name = pl.getName().getString();
        Set<String> s = names();
        boolean added = s.add(name.toLowerCase());
        if (!added) s.remove(name.toLowerCase());
        list.set(String.join(",", s));
        ModuleManager.INSTANCE.save();
        mc.player.sendSystemMessage(Component.literal("[Nimbus] " + (added ? "Added " : "Removed ") + name + (added ? " to" : " from") + " friends")); // CHECK
    }

    @Override
    public void onTick() {
        long h = GLFW.glfwGetCurrentContext();
        boolean down = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        if (down && !mmbDown && friends.get() && middleClick.get() && mc.gui.screen() == null // CHECK
                && mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Player pl) {
            toggleFriend(pl);
        }
        mmbDown = down;
    }
}