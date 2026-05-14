package com.smd.bulletapi.common.summon;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.api.summon.AbstractSummonBlueprint;
import com.smd.bulletapi.common.summon.blueprint.impl.FairyOrbBlueprint;
import com.smd.bulletapi.common.summon.blueprint.impl.LaserEyeBlueprint;
import com.smd.bulletapi.common.summon.blueprint.impl.RamWispBlueprint;
import com.smd.bulletapi.event.lifecycle.SummonDefinitionRegisteredEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@InternalApi
public final class SummonRegistry {
    private static final Map<String, SummonDefinition> REGISTRY = new ConcurrentHashMap<>();
    private static final Map<String, AbstractSummonBlueprint> BLUEPRINTS = new ConcurrentHashMap<>();
    private static boolean bootstrapped;

    private SummonRegistry() {}

    public static void register(SummonDefinition definition) {
        REGISTRY.put(definition.getId(), definition);
        MinecraftForge.EVENT_BUS.post(new SummonDefinitionRegisteredEvent(definition));
    }

    public static void register(AbstractSummonBlueprint blueprint) {
        if (blueprint == null) {
            throw new IllegalArgumentException("Summon blueprint must not be null");
        }
        BLUEPRINTS.put(blueprint.getId(), blueprint);
        register(blueprint.createDefinition());
    }

    public static SummonDefinition get(String id) {
        SummonDefinition definition = REGISTRY.get(id);
        return definition == null ? null : definition.copy();
    }

    public static AbstractSummonBlueprint getBlueprint(String id) {
        return BLUEPRINTS.get(id);
    }

    public static boolean has(String id) {
        return REGISTRY.containsKey(id);
    }

    public static void bootstrapDefaults() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;

        register(new FairyOrbBlueprint());
        register(new LaserEyeBlueprint());
        register(new RamWispBlueprint());
    }
}
