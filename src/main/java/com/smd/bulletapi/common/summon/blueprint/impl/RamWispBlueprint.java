package com.smd.bulletapi.common.summon.blueprint.impl;

import com.smd.bulletapi.common.collision.SphereShape;
import com.smd.bulletapi.api.summon.AbstractContactSummonBlueprint;
import com.smd.bulletapi.common.summon.SummonDefinition;
import com.smd.bulletapi.common.summon.SummonPresetKeys;
import com.smd.bulletapi.common.summon.behavior.impl.RamStrikeMoveController;
import net.minecraft.nbt.NBTTagCompound;

public class RamWispBlueprint extends AbstractContactSummonBlueprint {
    public RamWispBlueprint() {
        super(SummonPresetKeys.RAM_WISP);
    }

    @Override
    protected void configure(SummonDefinition.Builder builder) {
        NBTTagCompound ramWispData = new NBTTagCompound();
        ramWispData.setFloat("scale", 0.72f);

        builder.life(6000)
                .damage(3.5f)
                .slotCost(1)
                .texture("bulletapi:textures/entity/bullet.png")
                .color(0xFFD67A)
                .size(0.62f)
                .rendererType("billboard")
                .customData(ramWispData)
                .followRange(30.0)
                .attackRange(18.0)
                .leashRange(34.0)
                .moveSpeed(0.36)
                .acceleration(0.22)
                .idleHeight(1.05)
                .idleRadius(1.45)
                .retargetIntervalTicks(4)
                .syncIntervalTicks(1)
                .bodyCollisionIntervalTicks(2)
                .collisionShape(new SphereShape(0.28))
                .moveController(new RamStrikeMoveController());
    }
}
