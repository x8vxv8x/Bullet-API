package com.smd.bulletapi.common.summon.blueprint.impl;

import com.smd.bulletapi.common.collision.SphereShape;
import com.smd.bulletapi.api.summon.AbstractOrbitingSummonBlueprint;
import com.smd.bulletapi.common.data.DataPayload;
import com.smd.bulletapi.common.summon.SummonDefinition;
import com.smd.bulletapi.common.summon.SummonPresetKeys;
import com.smd.bulletapi.common.summon.behavior.impl.ShootBulletPattern;

public class FairyOrbBlueprint extends AbstractOrbitingSummonBlueprint {
    public FairyOrbBlueprint() {
        super(SummonPresetKeys.FAIRY_ORB);
    }

    @Override
    protected void configure(SummonDefinition.Builder builder) {
        DataPayload fairyData = new DataPayload();
        fairyData.setFloat("scale", 0.85f);

        builder.life(6000)
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
                .attackPattern(new ShootBulletPattern(
                        16, 52, 0.72, 2.25f,
                        "bulletapi:textures/entity/bullet.png",
                        0xCCFFFF, 0.42f, "billboard"
                ));
    }
}
