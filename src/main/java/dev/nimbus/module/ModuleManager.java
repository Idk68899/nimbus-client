package dev.nimbus.module;

import com.google.gson.*;
import dev.nimbus.module.impl.*;
import dev.nimbus.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("nimbus.json");

    private final List<Module> modules = new ArrayList<>();
    private final Set<Integer> down = new HashSet<>();

    public void init() {
        modules.add(new KillAura());
        modules.add(new Velocity());
        modules.add(new Sprint());
        modules.add(new Flight());
        modules.add(new Speed());
        modules.add(new Fullbright());
        modules.add(new Esp());
        load();
    }

    public List<Module> all() { return modules; }

    public List<Module> byCategory(String cat) {
        return modules.stream().filter(m -> m.category.equals(cat)).toList();
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> c) {
        for (Module m : modules) if (c.isInstance(m)) return (T) m;
        return null;
    }

    public void tick(Minecraft mc) {
        if (mc.player == null) return;
        long h = GLFW.glfwGetCurrentContext();
        boolean noScreen = mc.gui.screen() == null; // CHECK: gui.screen()
        for (Module m : modules) {
            if (m.key >= 0 && noScreen) {
                boolean now = GLFW.glfwGetKey(h, m.key) == GLFW.GLFW_PRESS;
                if (now && down.add(m.key)) m.toggle();
                else if (!now) down.remove(m.key);
            }
            if (m.isEnabled()) m.onTick();
        }
    }

    public void save() {
        try {
            JsonObject root = new JsonObject();
            for (Module m : modules) {
                JsonObject o = new JsonObject();
                o.addProperty("key", m.key);
                JsonObject s = new JsonObject();
                for (Setting<?> st : m.settings) s.add(st.name, st.save());
                o.add("settings", s);
                root.add(m.name, o);
            }
            Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ignored) {}
    }

    private void load() {
        if (!Files.exists(FILE)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            for (Module m : modules) {
                if (!root.has(m.name)) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                if (o.has("key")) m.key = o.get("key").getAsInt();
                if (!o.has("settings")) continue;
                JsonObject s = o.getAsJsonObject("settings");
                for (Setting<?> st : m.settings) if (s.has(st.name)) st.load(s.get(st.name));
            }
        } catch (Exception ignored) {}
    }
}