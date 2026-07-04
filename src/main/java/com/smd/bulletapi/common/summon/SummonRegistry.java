package com.smd.bulletapi.common.summon;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.api.summon.SummonType;
import com.smd.bulletapi.common.summon.type.impl.FairyOrbType;
import com.smd.bulletapi.common.summon.type.impl.LaserEyeType;
import com.smd.bulletapi.common.summon.type.impl.RamWispType;
import com.smd.bulletapi.event.lifecycle.SummonTypeRegisteredEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@InternalApi
public final class SummonRegistry {
    private static final Map<String, SummonType> REGISTRY = new ConcurrentHashMap<>();
    private static boolean bootstrapped;

    private SummonRegistry() {}

    public static void register(SummonType type) {
        if (type == null) {
            throw new IllegalArgumentException("Summon type must not be null");
        }
        REGISTRY.put(type.getId(), type);
        MinecraftForge.EVENT_BUS.post(new SummonTypeRegisteredEvent(type));
    }

    public static SummonType get(String id) {
        return id == null ? null : REGISTRY.get(id);
    }

    public static boolean has(String id) {
        return REGISTRY.containsKey(id);
    }

    public static void bootstrapDefaults() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        register(new FairyOrbType());
        register(new LaserEyeType());
        register(new RamWispType());
    }
}
