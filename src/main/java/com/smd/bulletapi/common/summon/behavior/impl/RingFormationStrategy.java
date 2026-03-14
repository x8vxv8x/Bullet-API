package com.smd.bulletapi.common.summon.behavior.impl;

import com.smd.bulletapi.common.summon.SummonContext;
import com.smd.bulletapi.common.summon.behavior.IFormationStrategy;
import net.minecraft.util.math.Vec3d;

public class RingFormationStrategy implements IFormationStrategy {
    @Override
    public Vec3d getAnchorPosition(SummonContext context) {
        int count = context.getOwnedSummonCount();
        double angle = (context.worldTick * 0.08D) + ((Math.PI * 2.0D) * context.summon.getFormationIndex() / count);
        double radius = context.definition.getIdleRadius();
        Vec3d ownerCenter = context.getOwnerCenter();
        return ownerCenter.add(Math.cos(angle) * radius, context.definition.getIdleHeight(), Math.sin(angle) * radius);
    }
}
