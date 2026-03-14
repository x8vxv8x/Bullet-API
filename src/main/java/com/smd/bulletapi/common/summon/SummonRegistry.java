package com.smd.bulletapi.common.summon;

import com.smd.bulletapi.common.collision.SphereShape;
import com.smd.bulletapi.common.summon.behavior.impl.NearestHostileSelector;
import com.smd.bulletapi.common.summon.behavior.impl.OrbitOwnerMoveController;
import com.smd.bulletapi.common.summon.behavior.impl.RingFormationStrategy;
import com.smd.bulletapi.common.summon.behavior.impl.ShootBulletPattern;
import com.smd.bulletapi.common.summon.behavior.impl.ShootLaserPattern;
import net.minecraft.init.SoundEvents;
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
                .bodyCollisionIntervalTicks(8)
                .collisionShape(new SphereShape(0.65))
                .targetSelector(new NearestHostileSelector())
                .formationStrategy(new RingFormationStrategy())
                .moveController(new OrbitOwnerMoveController())
                .attackPattern(new ShootBulletPattern(
                        16, 52, 0.72, 2.25f,
                        "bulletapi:textures/entity/bullet.png",
                        0xCCFFFF, 0.42f, "billboard"
                )));

        NBTTagCompound laserEyeData = new NBTTagCompound();
        laserEyeData.setFloat("scale", 1.1f);
        laserEyeData.setInteger("tint", 0xA8F2FF);

        register(new SummonDefinition(SummonPresetKeys.LASER_EYE)
                .life(6000)
                .damage(3.0f)
                .slotCost(2)
                .size(0.9f)
                .rendererType("model_json")
                .customData(laserEyeData)
                .followRange(28.0)
                .attackRange(24.0)
                .leashRange(32.0)
                .moveSpeed(0.24)
                .acceleration(0.13)
                .idleHeight(1.55)
                .idleRadius(2.2)
                .retargetIntervalTicks(8)
                .syncIntervalTicks(1)
                .bodyCollisionIntervalTicks(0)
                .collisionShape(null)
                .targetSelector(new NearestHostileSelector())
                .formationStrategy(new RingFormationStrategy())
                .moveController(new OrbitOwnerMoveController())
                .attackPattern(new ShootLaserPattern(
                        18, -1, 32.0, 0.8f, 3.0f, 0x66CCFF,
                        "laser_poly", 5, true, true,
                        ShootLaserPattern.soundOnHit(SoundEvents.BLOCK_NOTE_HARP, 0.4F, 1.8F),
                        builder -> builder
                                .set("alpha", 0.8f)
                                .set("poly_sides", 8)
                                .set("core_scale", 0.45f)
                                .set("shell_scale", 1.05f)
                                .set("pulse_amp", 0.18f)
                                .set("pulse_speed", 0.4f)
                                .set("core_color", 0xFFFFFF)
                                .set("shell_color", 0x66CCFF)
                                .set("shell_color_end", 0x3366FF)
                                .set("block_len", 3.2f)
                                .set("block_speed", 0.4f)
                                .set("block_soft", true)
                                .set("block_apply_core", false)
                                .set("block_color_a", 0x66CCFF)
                                .set("block_color_b", 0xFF66CC)
                                .set("twist_speed", 0.8f)
                                .set("twist_step", 0.4f)
                                .set("jitter_amp", 0.04f)
                                .set("jitter_freq", 0.9f)
                                .set("deco_on", true)
                                .set("deco_scale", 3.35f)
                                .set("deco_alpha", 0.45f)
                                .set("deco_step", 1.6f)
                                .set("deco_scroll", 0.1f)
                                .set("deco_rot_speed", 0.2f)
                                .set("deco_color", 0x88CCFF)
                ))
                .set("model", "minecraft:beacon")
                .set("variant", "inventory")
                .set("scale", 0.42f)
                .set("rot_mode", "face_camera")
                .set("tint", 0xA8F2FF));
    }
}
