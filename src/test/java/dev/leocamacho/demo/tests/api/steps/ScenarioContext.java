package dev.leocamacho.demo.tests.api.steps;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ScenarioContext {
    Map<Class, Map<String, Object>> data = new HashMap<>();


    public <T> void set(T value) {
        var instances = data.computeIfAbsent(value.getClass(), k -> new HashMap<>());
        instances.put((instances.size() + 1) + "", value);
    }

    public <T> void set(String name, T value) {
        var instances = data.computeIfAbsent(value.getClass(), k -> new HashMap<>());
        instances.put(name, value);
    }

    public <T> void setWithPrefix(String name, T value) {
        var instances = data.computeIfAbsent(value.getClass(), k -> new HashMap<>());
        var next = instances.keySet().stream().filter(k -> k.startsWith(name)).count() + 1;
        instances.put(next + "." + name, value);
    }

    public <T> T get(Class<T> clazz) {
        var types = data.get(clazz);
        if (types == null) {
            return null;
        }
        var result = types.entrySet().stream().findFirst();
        return result.map(stringObjectEntry -> clazz.cast(stringObjectEntry.getValue())).orElse(null);

    }

    public <T> T get(String name, Class<T> clazz) {
        return clazz.cast(data.get(clazz).get(name));
    }

    public <T> Set<T> getAll(Class<T> clazz) {
        return (Set<T>) data.get(clazz).values();
    }

    public <T> Set<T> getAllOfPrefix(String prefix, Class<T> clazz) {
        return (Set<T>) data.get(clazz).entrySet().stream().filter(e -> e.getKey().startsWith(prefix)).map(Map.Entry::getValue);
    }


}
