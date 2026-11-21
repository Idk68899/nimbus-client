package dev.nimbus.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Hidden string storage (used for the friends list). */
public class StringSetting extends Setting<String> {
    public StringSetting(String name, String def) {
        super(name, def);
        visible = () -> false;
    }

    public void set(String v) { value = v; }

    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement e) { value = e.getAsString(); }
}