package dev.nimbus.module;

import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public final class Profiles {
    private static Path dir() {
        Path d = FabricLoader.getInstance().getConfigDir().resolve("nimbus-profiles");
        try { Files.createDirectories(d); } catch (Exception ignored) {}
        return d;
    }

    public static List<String> list() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir())) {
            s.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".json"))
                    .forEach(n -> out.add(n.substring(0, n.length() - 5)));
        } catch (Exception ignored) {}
        Collections.sort(out);
        return out;
    }

    public static void save(String name) { ModuleManager.INSTANCE.saveTo(dir().resolve(name + ".json"), true); }
    public static void load(String name) { ModuleManager.INSTANCE.loadFrom(dir().resolve(name + ".json"), true); }
    public static void delete(String name) { try { Files.deleteIfExists(dir().resolve(name + ".json")); } catch (Exception ignored) {} }

    public static String newName() {
        List<String> existing = list();
        int i = 1;
        while (existing.contains("profile-" + i)) i++;
        return "profile-" + i;
    }
}