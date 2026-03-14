package com.smd.bulletapi.api.preset;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.event.lifecycle.BulletPresetRegisteredEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@PublicApi
public final class BulletPresetRegistry {
    private static final Map<String, BulletPreset> REGISTRY = new ConcurrentHashMap<>();

    private BulletPresetRegistry() {}

    public static void register(String id, BulletPreset preset) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Bullet preset id must not be empty");
        }
        if (preset == null) {
            throw new IllegalArgumentException("Bullet preset must not be null");
        }
        REGISTRY.put(id, preset);
        MinecraftForge.EVENT_BUS.post(new BulletPresetRegisteredEvent(id, preset));
    }

    public static BulletPreset get(String id) {
        return REGISTRY.get(id);
    }

    public static boolean has(String id) {
        return REGISTRY.containsKey(id);
    }
}
