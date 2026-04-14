package com.smd.bulletapi.common.summon.behavior.impl;

import com.smd.bulletapi.common.summon.SummonContext;
import com.smd.bulletapi.common.summon.SummonState;
import com.smd.bulletapi.common.summon.behavior.IFormationStrategy;
import com.smd.bulletapi.common.summon.behavior.ISummonMoveController;
import com.smd.bulletapi.common.summon.SummonPresetKeys;
import com.smd.bulletapi.api.runtime.ISummonActor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.HashMap;

public class RamStrikeMoveController implements ISummonMoveController {
    public static final String MODE_BODY_HIT = SummonPresetKeys.RAM_WISP + "/body_hit";
    public static final String KEY_HIT_ENTITY_ID = "hit_entity_id";

    private static final Map<Integer, RamState> STATES = new HashMap<>();

    private final double dashSpeed;
    private final int dashTicks;
    private final double repositionDistance;
    private final int penetrateTicks;
    private final int reacquireDelayTicks;
    private final double approachDistance;
    private final double attackAccel;
    private final double idleDamping;

    public RamStrikeMoveController() {
        this(1.45D, 18, 2.4D, 5, 3, 1.15D, 0.34D, 0.82D);
    }

    public RamStrikeMoveController(double dashSpeed, int dashTicks, double repositionDistance,
                                   int penetrateTicks, int reacquireDelayTicks,
                                   double approachDistance, double attackAccel, double idleDamping) {
        this.dashSpeed = dashSpeed;
        this.dashTicks = Math.max(2, dashTicks);
        this.repositionDistance = Math.max(0.5D, repositionDistance);
        this.penetrateTicks = Math.max(1, penetrateTicks);
        this.reacquireDelayTicks = Math.max(0, reacquireDelayTicks);
        this.approachDistance = Math.max(0.2D, approachDistance);
        this.attackAccel = clamp01(attackAccel);
        this.idleDamping = clamp01(idleDamping);
    }

    @Override
    public void tickNoTargetMovement(SummonContext context) {
        ISummonActor summon = context.summon;
        RamState state = STATES.computeIfAbsent(summon.getId(), ignored -> new RamState());
        state.lastActiveTick = context.worldTick;
        pruneStaleStates(context.worldTick);
        consumeBodyHit(context);

        Vec3d ownerCenter = context.getOwnerCenter();
        Vec3d current = summon.getPosition();
        double leashSq = context.definition.getLeashRange() * context.definition.getLeashRange();
        if (current.squareDistanceTo(ownerCenter) > leashSq) {
            state.resetCombat();
            Vec3d toOwner = ownerCenter.subtract(current);
            if (toOwner.lengthSquared() > 1.0E-6) {
                summon.setVelocity(toOwner.normalize().scale(Math.max(dashSpeed, context.definition.getMoveSpeed() * 2.6D)));
            } else {
                summon.setVelocity(summon.getVelocity().scale(idleDamping));
            }
            summon.setState(SummonState.RETURNING);
            return;
        }

        state.resetCombat();
        followOwner(context);
    }

    @Override
    public void tickCombatMovement(SummonContext context) {
        ISummonActor summon = context.summon;
        RamState state = STATES.computeIfAbsent(summon.getId(), ignored -> new RamState());
        state.lastActiveTick = context.worldTick;
        pruneStaleStates(context.worldTick);
        consumeBodyHit(context);

        EntityLivingBase target = context.getTarget();
        if (target == null || target.isDead) {
            tickNoTargetMovement(context);
            return;
        }

        if (state.targetId != target.getEntityId()) {
            state.targetId = target.getEntityId();
            state.phase = Phase.APPROACH;
            state.phaseTicks = 0;
            state.penetrateTicksLeft = 0;
            state.reacquireTicksLeft = 0;
        }

        switch (state.phase) {
            case PENETRATING:
                tickPenetrating(context, state);
                return;
            case DASHING:
                tickDashing(context, state);
                return;
            case REACQUIRING:
                tickReacquiring(context, state, target);
                return;
            case APPROACH:
            default:
                tickApproach(context, state, target);
                return;
        }
    }

    private void consumeBodyHit(SummonContext context) {
        if (!MODE_BODY_HIT.equals(context.getMode()) || !context.hasRuntimeParam(KEY_HIT_ENTITY_ID)) {
            return;
        }
        int hitEntityId = context.consumeInt(KEY_HIT_ENTITY_ID, -1);
        context.clearMode();
        Entity entity = hitEntityId < 0 ? null : context.world.getEntityByID(hitEntityId);
        if (!(entity instanceof EntityLivingBase) || entity.isDead) {
            return;
        }
        applyBodyHit(context, (EntityLivingBase) entity);
    }

    private void applyBodyHit(SummonContext context, EntityLivingBase hitEntity) {
        ISummonActor summon = context.summon;
        RamState state = STATES.computeIfAbsent(summon.getId(), ignored -> new RamState());
        state.lastActiveTick = context.worldTick;
        state.lastHitTargetId = hitEntity.getEntityId();
        context.setTarget(hitEntity);

        if (state.phase == Phase.PENETRATING) {
            return;
        }

        Vec3d velocity = summon.getVelocity();
        if (state.phase == Phase.APPROACH && velocity.lengthSquared() < 0.36D) {
            return;
        }
        Vec3d direction = velocity.lengthSquared() > 1.0E-6
                ? velocity.normalize()
                : hitEntity.getPositionVector().add(0, hitEntity.height * 0.5D, 0).subtract(summon.getPosition()).normalize();
        if (direction.lengthSquared() < 1.0E-6) {
            direction = new Vec3d(0, 0, 1);
        }

        state.dashDirection = direction;
        state.phase = Phase.PENETRATING;
        state.phaseTicks = 0;
        state.penetrateTicksLeft = state.penetrateDuration > 0 ? state.penetrateDuration : 4;
    }

    private void tickApproach(SummonContext context, RamState state, EntityLivingBase target) {
        Vec3d current = context.summon.getPosition();
        Vec3d targetCenter = context.getTargetCenter();
        Vec3d ownerCenter = context.getOwnerCenter();
        Vec3d desired = computeApproachPoint(targetCenter, ownerCenter, target.width * 0.5D + approachDistance);

        moveToward(context, desired, Math.max(context.definition.getMoveSpeed() * 1.25D, 0.42D), attackAccel);
        context.summon.setState(SummonState.CHASING_TARGET);
        state.phaseTicks++;

        double distSqToTarget = target.getDistanceSq(current.x, current.y, current.z);
        double armDistance = Math.max(1.0D, target.width + approachDistance * 1.5D);
        if (distSqToTarget <= armDistance * armDistance || state.phaseTicks >= 10) {
            beginDash(context, state, targetCenter);
        }
    }

    private void tickDashing(SummonContext context, RamState state) {
        context.summon.setVelocity(state.dashDirection.scale(dashSpeed));
        context.summon.setState(SummonState.ATTACKING);
        state.phaseTicks++;
        if (state.phaseTicks >= state.dashDuration) {
            state.phase = Phase.REACQUIRING;
            state.phaseTicks = 0;
            state.reacquireTicksLeft = reacquireDelayTicks;
        }
    }

    private void tickPenetrating(SummonContext context, RamState state) {
        context.summon.setVelocity(state.dashDirection.scale(dashSpeed));
        context.summon.setState(SummonState.ATTACKING);
        state.phaseTicks++;
        state.penetrateTicksLeft--;
        if (state.penetrateTicksLeft <= 0) {
            state.phase = Phase.REACQUIRING;
            state.phaseTicks = 0;
            state.reacquireTicksLeft = reacquireDelayTicks;
        }
    }

    private void tickReacquiring(SummonContext context, RamState state, EntityLivingBase target) {
        if (state.reacquireTicksLeft > 0) {
            state.reacquireTicksLeft--;
            context.summon.setVelocity(context.summon.getVelocity().scale(0.88D));
            context.summon.setState(SummonState.RETURNING);
            return;
        }

        Vec3d current = context.summon.getPosition();
        Vec3d targetCenter = context.getTargetCenter();
        Vec3d away = current.subtract(targetCenter);
        if (away.lengthSquared() < 1.0E-6) {
            away = state.dashDirection.scale(-1.0D);
        }
        Vec3d desired = targetCenter.add(away.normalize().scale(repositionDistance));
        moveToward(context, desired, Math.max(context.definition.getMoveSpeed() * 1.65D, 0.55D), attackAccel);
        context.summon.setState(SummonState.RETURNING);
        state.phaseTicks++;

        double targetDistance = current.distanceTo(targetCenter);
        if (targetDistance <= repositionDistance + 0.55D || state.phaseTicks >= 8) {
            beginDash(context, state, targetCenter);
        }
    }

    private void beginDash(SummonContext context, RamState state, Vec3d targetCenter) {
        Vec3d current = context.summon.getPosition();
        Vec3d dashDir = targetCenter.subtract(current);
        if (dashDir.lengthSquared() < 1.0E-6) {
            dashDir = context.summon.getVelocity();
        }
        if (dashDir.lengthSquared() < 1.0E-6) {
            dashDir = new Vec3d(0, 0, 1);
        }

        state.dashDirection = dashDir.normalize();
        state.phase = Phase.DASHING;
        state.phaseTicks = 0;
        state.dashDuration = dashTicks;
        state.penetrateDuration = penetrateTicks;
        context.summon.setVelocity(state.dashDirection.scale(dashSpeed));
        context.summon.setState(SummonState.ATTACKING);
    }

    private void followOwner(SummonContext context) {
        IFormationStrategy strategy = context.definition.getFormationStrategy();
        Vec3d desired = strategy == null ? context.getOwnerCenter() : strategy.getAnchorPosition(context);
        Vec3d current = context.summon.getPosition();
        Vec3d toDesired = desired.subtract(current);
        if (toDesired.lengthSquared() < 1.0E-6) {
            context.summon.setVelocity(context.summon.getVelocity().scale(idleDamping));
            context.summon.setState(SummonState.FOLLOW_OWNER);
            return;
        }

        double speed = Math.min(context.definition.getMoveSpeed() + Math.sqrt(toDesired.lengthSquared()) * 0.04D,
                context.definition.getMoveSpeed() * 2.0D);
        Vec3d desiredVelocity = toDesired.normalize().scale(speed);
        Vec3d currentVelocity = context.summon.getVelocity();
        double accel = context.definition.getAcceleration();
        context.summon.setVelocity(currentVelocity.scale(1.0D - accel).add(desiredVelocity.scale(accel)));
        context.summon.setState(SummonState.FOLLOW_OWNER);
    }

    private void moveToward(SummonContext context, Vec3d desiredPosition, double speed, double accel) {
        Vec3d current = context.summon.getPosition();
        Vec3d toDesired = desiredPosition.subtract(current);
        if (toDesired.lengthSquared() < 1.0E-6) {
            context.summon.setVelocity(context.summon.getVelocity().scale(idleDamping));
            return;
        }

        Vec3d desiredVelocity = toDesired.normalize().scale(speed);
        Vec3d currentVelocity = context.summon.getVelocity();
        context.summon.setVelocity(currentVelocity.scale(1.0D - accel).add(desiredVelocity.scale(accel)));
    }

    private Vec3d computeApproachPoint(Vec3d targetCenter, Vec3d ownerCenter, double offset) {
        Vec3d fromOwner = targetCenter.subtract(ownerCenter);
        if (fromOwner.lengthSquared() < 1.0E-6) {
            fromOwner = new Vec3d(1, 0, 0);
        }
        Vec3d lateral = new Vec3d(-fromOwner.z, 0, fromOwner.x);
        if (lateral.lengthSquared() < 1.0E-6) {
            lateral = new Vec3d(1, 0, 0);
        }
        lateral = lateral.normalize();
        return targetCenter.add(lateral.scale(offset)).add(0, 0.12D, 0);
    }

    private void pruneStaleStates(long worldTick) {
        STATES.entrySet().removeIf(entry -> worldTick - entry.getValue().lastActiveTick > 200L);
    }

    private static double clamp01(double value) {
        if (value < 0.0D) return 0.0D;
        if (value > 1.0D) return 1.0D;
        return value;
    }

    private enum Phase {
        APPROACH,
        DASHING,
        PENETRATING,
        REACQUIRING
    }

    private static class RamState {
        private Phase phase = Phase.APPROACH;
        private Vec3d dashDirection = new Vec3d(0, 0, 1);
        private int targetId = -1;
        private int lastHitTargetId = -1;
        private int phaseTicks;
        private int dashDuration = 18;
        private int penetrateDuration = 4;
        private int penetrateTicksLeft;
        private int reacquireTicksLeft;
        private long lastActiveTick;

        private void resetCombat() {
            phase = Phase.APPROACH;
            phaseTicks = 0;
            targetId = -1;
            lastHitTargetId = -1;
            penetrateTicksLeft = 0;
            reacquireTicksLeft = 0;
            dashDirection = new Vec3d(0, 0, 1);
        }
    }
}
