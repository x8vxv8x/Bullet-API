package com.smd.bulletapi.api.preset;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.event.lifecycle.LaserPresetRegisteredEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@PublicApi
public final class LaserPresetRegistry {
    private static final Map<String, LaserPreset> REGISTRY = new ConcurrentHashMap<>();

    private LaserPresetRegistry() {}

    public static void register(String id, LaserPreset preset) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Laser preset id must not be empty");
        }
        if (preset == null) {
            throw new IllegalArgumentException("Laser preset must not be null");
        }
        REGISTRY.put(id, preset);
        MinecraftForge.EVENT_BUS.post(new LaserPresetRegisteredEvent(id, preset));
    }

    public static LaserPreset get(String id) {
        return REGISTRY.get(id);
    }

    public static boolean has(String id) {
        return REGISTRY.containsKey(id);
    }
}
