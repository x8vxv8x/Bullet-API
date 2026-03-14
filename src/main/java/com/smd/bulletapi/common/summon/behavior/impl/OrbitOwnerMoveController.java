package com.smd.bulletapi.common.summon.behavior.impl;

import com.smd.bulletapi.common.summon.SummonContext;
import com.smd.bulletapi.common.summon.SummonState;
import com.smd.bulletapi.common.summon.behavior.IFormationStrategy;
import com.smd.bulletapi.common.summon.behavior.ISummonMoveController;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;

public class OrbitOwnerMoveController implements ISummonMoveController {
    @Override
    public void tickMovement(SummonContext context) {
        EntityLivingBase target = context.getTarget();
        Vec3d desiredPosition;
        if (target != null && !target.isDead) {
            double angle = (context.worldTick * 0.14D) + context.summon.getFormationIndex();
            Vec3d targetCenter = context.getTargetCenter();
            double radius = Math.max(1.2D, context.definition.getIdleRadius() * 0.85D);
            desiredPosition = targetCenter.add(Math.cos(angle) * radius, 0.8D + Math.sin(angle * 0.5D) * 0.25D, Math.sin(angle) * radius);
            context.summon.setState(SummonState.CHASING_TARGET);
        } else {
            IFormationStrategy strategy = context.definition.getFormationStrategy();
            desiredPosition = strategy == null ? context.getOwnerCenter() : strategy.getAnchorPosition(context);
            context.summon.setState(SummonState.FOLLOW_OWNER);
        }

        Vec3d current = context.summon.getPosition();
        Vec3d toDesired = desiredPosition.subtract(current);
        double distance = toDesired.length();
        if (distance < 1.0E-4) {
            context.summon.setVelocity(context.summon.getVelocity().scale(0.65));
            return;
        }

        double speed = Math.min(context.definition.getMoveSpeed() + distance * 0.02D, context.definition.getMoveSpeed() * 2.1D);
        Vec3d desiredVelocity = toDesired.normalize().scale(speed);
        Vec3d currentVelocity = context.summon.getVelocity();
        double accel = context.definition.getAcceleration();
        Vec3d nextVelocity = currentVelocity.scale(1.0D - accel).add(desiredVelocity.scale(accel));

        double leashSq = context.definition.getLeashRange() * context.definition.getLeashRange();
        if (current.squareDistanceTo(context.getOwnerCenter()) > leashSq) {
            nextVelocity = context.getOwnerCenter().subtract(current).normalize().scale(context.definition.getMoveSpeed() * 2.5D);
            context.summon.setState(SummonState.RETURNING);
        }

        context.summon.setVelocity(nextVelocity);
    }
}
