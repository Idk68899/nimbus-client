package dev.nimbus.setting;

import com.google.gson.JsonElement;
import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    public final String name;
    public String group = "General";
    public BooleanSupplier visible = () -> true;
    protected T value;
    protected final T def;

    protected Setting(String name, T value) {
        this.name = name;
        this.value = value;
        this.def = value;
    }

    public T get() { return value; }
    public void reset() { value = def; }
    public abstract JsonElement save();
    public abstract void load(JsonElement e);
}