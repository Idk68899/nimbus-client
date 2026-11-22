package dev.nimbus.module;

import com.google.gson.*;
import dev.nimbus.module.impl.*;
import dev.nimbus.setting.Setting;
import dev.nimbus.util.Sync;
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
        // Combat
        modules.add(new KillAura());
        modules.add(new Reach());
        modules.add(new Hitbox());
        modules.add(new AimAssist());
        modules.add(new TriggerBot());
        modules.add(new AutoClicker());
        modules.add(new AutoWeapon());
        modules.add(new BowSpam());
        modules.add(new AutoMace());
        modules.add(new MaceSmash());
        modules.add(new WindBoost());
        modules.add(new SpearCharge());
        modules.add(new CrystalAura());
        modules.add(new AnchorAura());
        modules.add(new Surround());
        modules.add(new SafeAnchor());
        modules.add(new AutoWeb());
        modules.add(new Velocity());
        // Player
        modules.add(new Teams());
        modules.add(new AutoTotem());
        modules.add(new AutoPot());
        modules.add(new AutoRefill());
        modules.add(new AutoGapple());
        modules.add(new AutoArmor());
        modules.add(new AutoEat());
        modules.add(new AutoXP());
        modules.add(new AutoPearl());
        modules.add(new AutoFirework());
        modules.add(new ElytraSwap());
        modules.add(new AutoTool());
        modules.add(new AutoRespawn());
        modules.add(new FastPlace());
        modules.add(new AntiAfk());
        modules.add(new PopCounter());
        // Movement
        modules.add(new Sprint());
        modules.add(new Speed());
        modules.add(new Bhop());
        modules.add(new Flight());
        modules.add(new ElytraFly());
        modules.add(new Step());
        modules.add(new Spider());
        modules.add(new SafeWalk());
        modules.add(new Scaffold());
        modules.add(new AutoWalk());
        // Render
        modules.add(new Fullbright());
        modules.add(new Esp());
        modules.add(new TargetOutline());
        modules.add(new Zoom());
        modules.add(new HudModule());
        modules.add(new Radar());
        modules.add(new TargetHud());
        modules.add(new ArmorHud());
        modules.add(new PotionHud());
        modules.add(new Keystrokes());
        loadFrom(FILE, false);
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
        Sync.tick++;
        long h = GLFW.glfwGetCurrentContext();
        boolean noScreen = mc.gui.screen() == null; // CHECK: gui.screen()
        for (Module m : new ArrayList<>(modules)) {
            if (m.key >= 0 && noScreen) {
                boolean now = GLFW.glfwGetKey(h, m.key) == GLFW.GLFW_PRESS;
                if (now && down.add(m.key)) m.toggle();
                else if (!now) down.remove(m.key);
            }
            if (m.isEnabled()) m.onTick();
        }
    }

    public void save() { saveTo(FILE, false); }

    public void saveTo(Path file, boolean withState) {
        try {
            JsonObject root = new JsonObject();
            for (Module m : modules) {
                JsonObject o = new JsonObject();
                o.addProperty("key", m.key);
                if (withState) o.addProperty("enabled", m.isEnabled());
                JsonObject s = new JsonObject();
                for (Setting<?> st : m.settings) s.add(st.name, st.save());
                o.add("settings", s);
                root.add(m.name, o);
            }
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ignored) {}
    }

    public void loadFrom(Path file, boolean withState) {
        if (!Files.exists(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            for (Module m : modules) {
                if (!root.has(m.name)) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                if (o.has("key")) m.key = o.get("key").getAsInt();
                if (o.has("settings")) {
                    JsonObject s = o.getAsJsonObject("settings");
                    for (Setting<?> st : m.settings) if (s.has(st.name)) st.load(s.get(st.name));
                }
                if (withState && o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean());
            }
        } catch (Exception ignored) {}
    }
}