package com.smd.bulletapi.common.summon.blueprint.impl;

import com.smd.bulletapi.api.summon.AbstractOrbitingSummonBlueprint;
import com.smd.bulletapi.common.summon.SummonDefinition;
import com.smd.bulletapi.common.summon.SummonPresetKeys;
import com.smd.bulletapi.common.summon.behavior.impl.ShootLaserPattern;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;

public class LaserEyeBlueprint extends AbstractOrbitingSummonBlueprint {
    public LaserEyeBlueprint() {
        super(SummonPresetKeys.LASER_EYE);
    }

    @Override
    protected void configure(SummonDefinition.Builder builder) {
        NBTTagCompound laserEyeData = new NBTTagCompound();
        laserEyeData.setFloat("scale", 0.42f);
        laserEyeData.setInteger("tint", 0xA8F2FF);
        laserEyeData.setString("model", "minecraft:beacon");
        laserEyeData.setString("variant", "inventory");
        laserEyeData.setString("rot_mode", "face_camera");

        builder.life(6000)
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
                .attackPattern(new ShootLaserPattern(
                        18, -1, 32.0, 0.8f, 3.0f, 0x66CCFF,
                        "laser_poly", 5, true, true,
                        ShootLaserPattern.soundOnHit(SoundEvents.BLOCK_NOTE_HARP, 0.4F, 1.8F),
                        laserBuilder -> laserBuilder
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
                ));
    }
}
