package com.smd.bulletapi.common.summon;

import com.smd.bulletapi.common.summon.behavior.impl.NearestHostileSelector;
import com.smd.bulletapi.common.summon.behavior.impl.OrbitOwnerMoveController;
import com.smd.bulletapi.common.summon.behavior.impl.RingFormationStrategy;
import com.smd.bulletapi.common.summon.behavior.impl.ShootBulletPattern;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SummonRegistry {
    private static final Map<String, SummonDefinition> REGISTRY = new ConcurrentHashMap<>();
    private static boolean bootstrapped;

    private SummonRegistry() {}

    public static void register(SummonDefinition definition) {
        REGISTRY.put(definition.getId(), definition);
    }

    public static SummonDefinition get(String id) {
        SummonDefinition definition = REGISTRY.get(id);
        return definition == null ? null : definition.copy();
    }

    public static boolean has(String id) {
        return REGISTRY.containsKey(id);
    }

    public static void bootstrapDefaults() {
        if (bootstrapped) return;
        bootstrapped = true;

        NBTTagCompound fairyData = new NBTTagCompound();
        fairyData.setFloat("scale", 0.85f);

        register(new SummonDefinition(SummonPresetKeys.FAIRY_ORB)
                .life(6000)
                .damage(2.5f)
                .slotCost(1)
                .texture("bulletapi:textures/entity/bullet.png")
                .color(0x7FE7FF)
                .size(0.85f)
                .rendererType("billboard")
                .customData(fairyData)
                .followRange(24.0)
                .attackRange(18.0)
                .leashRange(28.0)
                .moveSpeed(0.28)
                .acceleration(0.14)
                .idleHeight(1.35)
                .idleRadius(1.9)
                .retargetIntervalTicks(10)
                .syncIntervalTicks(2)
                .targetSelector(new NearestHostileSelector())
                .formationStrategy(new RingFormationStrategy())
                .moveController(new OrbitOwnerMoveController())
                .attackPattern(new ShootBulletPattern(
                        16, 52, 0.72, 2.25f,
                        "bulletapi:textures/entity/bullet.png",
                        0xCCFFFF, 0.42f, "billboard"
                )));
    }
}
