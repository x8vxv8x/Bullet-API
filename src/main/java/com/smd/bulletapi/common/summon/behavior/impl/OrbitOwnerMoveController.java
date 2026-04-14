package com.smd.bulletapi.common.summon.behavior.impl;

import com.smd.bulletapi.common.summon.SummonContext;
import com.smd.bulletapi.common.summon.SummonState;
import com.smd.bulletapi.common.summon.behavior.IFormationStrategy;
import com.smd.bulletapi.common.summon.behavior.ISummonMoveController;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;

public class OrbitOwnerMoveController implements ISummonMoveController {
    @Override
    public void tickNoTargetMovement(SummonContext context) {
        Vec3d ownerCenter = context.getOwnerCenter();
        Vec3d current = context.summon.getPosition();
        double leashSq = context.definition.getLeashRange() * context.definition.getLeashRange();

        Vec3d desiredPosition;
        double speedMultiplier;
        if (current.squareDistanceTo(ownerCenter) > leashSq) {
            desiredPosition = ownerCenter;
            speedMultiplier = 2.5D;
            context.summon.setState(SummonState.RETURNING);
        } else {
            IFormationStrategy strategy = context.definition.getFormationStrategy();
            desiredPosition = strategy == null ? ownerCenter : strategy.getAnchorPosition(context);
            speedMultiplier = 1.0D;
            context.summon.setState(SummonState.FOLLOW_OWNER);
        }

        applyMovement(context, desiredPosition, speedMultiplier, 0.65D);
    }

    @Override
    public void tickCombatMovement(SummonContext context) {
        EntityLivingBase target = context.getTarget();
        if (target == null || target.isDead) {
            tickNoTargetMovement(context);
            return;
        }

        double angle = (context.worldTick * 0.14D) + context.summon.getFormationIndex();
        Vec3d targetCenter = context.getTargetCenter();
        double radius = Math.max(1.2D, context.definition.getIdleRadius() * 0.85D);
        Vec3d desiredPosition = targetCenter.add(
                Math.cos(angle) * radius,
                0.8D + Math.sin(angle * 0.5D) * 0.25D,
                Math.sin(angle) * radius
        );

        context.summon.setState(SummonState.CHASING_TARGET);
        applyMovement(context, desiredPosition, 1.0D, 0.65D);
    }

    private void applyMovement(SummonContext context, Vec3d desiredPosition, double speedMultiplier, double damping) {
        Vec3d current = context.summon.getPosition();
        Vec3d toDesired = desiredPosition.subtract(current);
        double distance = toDesired.length();
        if (distance < 1.0E-4) {
            context.summon.setVelocity(context.summon.getVelocity().scale(damping));
            return;
        }

        double speed = Math.min(
                context.definition.getMoveSpeed() + distance * 0.02D,
                context.definition.getMoveSpeed() * (2.1D * Math.max(1.0D, speedMultiplier))
        );
        Vec3d desiredVelocity = toDesired.normalize().scale(speed);
        Vec3d currentVelocity = context.summon.getVelocity();
        double accel = context.definition.getAcceleration();
        context.summon.setVelocity(currentVelocity.scale(1.0D - accel).add(desiredVelocity.scale(accel)));
    }
}
