package com.smd.bulletapi.common.summon.type.impl;

import com.smd.bulletapi.api.summon.AbstractSummonEntity;
import com.smd.bulletapi.api.summon.SummonSpec;
import com.smd.bulletapi.api.summon.SummonType;
import com.smd.bulletapi.common.collision.SphereShape;
import com.smd.bulletapi.common.summon.SummonPresetKeys;
import com.smd.bulletapi.common.summon.SummonState;
import com.smd.bulletapi.common.summon.SummonTargetSource;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class RamWispType extends SummonType {
    public RamWispType() {
        super(SummonPresetKeys.RAM_WISP, createSpec());
    }

    private static SummonSpec createSpec() {
        return new SummonSpec()
                .life(6000)
                .damage(3.5f)
                .slotCost(1)
                .texture("bulletapi:textures/entity/bullet.png")
                .color(0xFFD67A)
                .size(0.62f)
                .rendererType("billboard")
                .followRange(30.0D)
                .attackRange(18.0D)
                .leashRange(34.0D)
                .moveSpeed(0.36D)
                .acceleration(0.22D)
                .idleHeight(1.05D)
                .idleRadius(1.45D)
                .retargetIntervalTicks(4)
                .syncIntervalTicks(1)
                .bodyCollisionIntervalTicks(2)
                .collisionShape(new SphereShape(0.28D))
                .set("scale", 0.72f);
    }

    @Override
    public AbstractSummonEntity createEntity(int id, World world, EntityLivingBase owner,
                                             Vec3d position, int formationIndex, long spawnTick) {
        return new RamWispEntity(id, this, owner, position, formationIndex, spawnTick);
    }

    private static final class RamWispEntity extends AbstractSummonEntity {
        private static final double DASH_SPEED = 1.45D;
        private static final int DASH_TICKS = 18;
        private static final double REPOSITION_DISTANCE = 2.4D;
        private static final int PENETRATE_TICKS = 5;
        private static final int REACQUIRE_DELAY_TICKS = 3;
        private static final double APPROACH_DISTANCE = 1.15D;
        private static final double ATTACK_ACCEL = 0.34D;
        private static final double IDLE_DAMPING = 0.82D;

        private Phase phase = Phase.APPROACH;
        private Vec3d dashDirection = new Vec3d(0, 0, 1);
        private int trackedTargetId = -1;
        private int phaseTicks;
        private int penetrateTicksLeft;
        private int reacquireTicksLeft;

        private RamWispEntity(int id, SummonType type, EntityLivingBase owner, Vec3d position,
                              int formationIndex, long spawnTick) {
            super(id, type, owner, position, formationIndex, spawnTick);
        }

        @Override
        public void tickServer(World world, EntityLivingBase owner, EntityLivingBase currentTarget) {
            if (owner == null || owner.isDead) {
                markDead();
                return;
            }

            if (isOutsideLeash(owner)) {
                resetCombatState();
                Vec3d toOwner = getOwnerCenter(owner).subtract(getPosition());
                if (toOwner.lengthSquared() > 1.0E-6D) {
                    setVelocity(toOwner.normalize().scale(Math.max(DASH_SPEED, getSpec().getMoveSpeed() * 2.6D)));
                } else {
                    setVelocity(getVelocity().scale(IDLE_DAMPING));
                }
                clearTarget();
                setState(SummonState.RETURNING);
                return;
            }

            EntityLivingBase target = currentTarget;
            if (target == null && shouldRetarget()) {
                target = acquireTarget(world, owner);
                resetRetargetCooldown();
            }

            if (target == null || target.isDead) {
                resetCombatState();
                followOwnerIdle(owner);
                return;
            }

            setTarget(target, SummonTargetSource.AUTO);
            if (trackedTargetId != target.getEntityId()) {
                trackedTargetId = target.getEntityId();
                phase = Phase.APPROACH;
                phaseTicks = 0;
                penetrateTicksLeft = 0;
                reacquireTicksLeft = 0;
            }

            switch (phase) {
                case PENETRATING:
                    tickPenetrating();
                    return;
                case DASHING:
                    tickDashing();
                    return;
                case REACQUIRING:
                    tickReacquiring(target);
                    return;
                case APPROACH:
                default:
                    tickApproach(owner, target);
            }
        }

        @Override
        public void onBodyCollision(World world, EntityLivingBase target) {
            if (target == null || target.isDead) {
                return;
            }
            setTarget(target, SummonTargetSource.SCRIPT);
            if (phase == Phase.PENETRATING) {
                return;
            }
            Vec3d velocity = getVelocity();
            if (phase == Phase.APPROACH && velocity.lengthSquared() < 0.36D) {
                return;
            }
            Vec3d direction = velocity.lengthSquared() > 1.0E-6D
                    ? velocity.normalize()
                    : getTargetCenter(target).subtract(getPosition()).normalize();
            if (direction.lengthSquared() < 1.0E-6D) {
                direction = new Vec3d(0, 0, 1);
            }
            dashDirection = direction;
            phase = Phase.PENETRATING;
            phaseTicks = 0;
            penetrateTicksLeft = PENETRATE_TICKS;
        }

        private void tickApproach(EntityLivingBase owner, EntityLivingBase target) {
            Vec3d desired = computeApproachPoint(getTargetCenter(target), getOwnerCenter(owner), target.width * 0.5D + APPROACH_DISTANCE);
            moveToward(desired, Math.max(getSpec().getMoveSpeed() * 1.25D, 0.42D), ATTACK_ACCEL);
            setState(SummonState.CHASING_TARGET);
            phaseTicks++;

            Vec3d current = getPosition();
            double distSqToTarget = target.getDistanceSq(current.x, current.y, current.z);
            double armDistance = Math.max(1.0D, target.width + APPROACH_DISTANCE * 1.5D);
            if (distSqToTarget <= armDistance * armDistance || phaseTicks >= 10) {
                beginDash(getTargetCenter(target));
            }
        }

        private void tickDashing() {
            setVelocity(dashDirection.scale(DASH_SPEED));
            setState(SummonState.ATTACKING);
            phaseTicks++;
            if (phaseTicks >= DASH_TICKS) {
                phase = Phase.REACQUIRING;
                phaseTicks = 0;
                reacquireTicksLeft = REACQUIRE_DELAY_TICKS;
            }
        }

        private void tickPenetrating() {
            setVelocity(dashDirection.scale(DASH_SPEED));
            setState(SummonState.ATTACKING);
            phaseTicks++;
            penetrateTicksLeft--;
            if (penetrateTicksLeft <= 0) {
                phase = Phase.REACQUIRING;
                phaseTicks = 0;
                reacquireTicksLeft = REACQUIRE_DELAY_TICKS;
            }
        }

        private void tickReacquiring(EntityLivingBase target) {
            if (reacquireTicksLeft > 0) {
                reacquireTicksLeft--;
                setVelocity(getVelocity().scale(0.88D));
                setState(SummonState.RETURNING);
                return;
            }

            Vec3d current = getPosition();
            Vec3d targetCenter = getTargetCenter(target);
            Vec3d away = current.subtract(targetCenter);
            if (away.lengthSquared() < 1.0E-6D) {
                away = dashDirection.scale(-1.0D);
            }
            Vec3d desired = targetCenter.add(away.normalize().scale(REPOSITION_DISTANCE));
            moveToward(desired, Math.max(getSpec().getMoveSpeed() * 1.65D, 0.55D), ATTACK_ACCEL);
            setState(SummonState.RETURNING);
            phaseTicks++;

            double targetDistance = current.distanceTo(targetCenter);
            if (targetDistance <= REPOSITION_DISTANCE + 0.55D || phaseTicks >= 8) {
                beginDash(targetCenter);
            }
        }

        private void beginDash(Vec3d targetCenter) {
            Vec3d current = getPosition();
            Vec3d dashDir = targetCenter.subtract(current);
            if (dashDir.lengthSquared() < 1.0E-6D) {
                dashDir = getVelocity();
            }
            if (dashDir.lengthSquared() < 1.0E-6D) {
                dashDir = new Vec3d(0, 0, 1);
            }

            dashDirection = dashDir.normalize();
            phase = Phase.DASHING;
            phaseTicks = 0;
            setVelocity(dashDirection.scale(DASH_SPEED));
            setState(SummonState.ATTACKING);
        }

        private void followOwnerIdle(EntityLivingBase owner) {
            Vec3d desired = getRingAnchor(owner, getSpec().getIdleRadius(), getSpec().getIdleHeight(), 0.12D);
            Vec3d current = getPosition();
            Vec3d toDesired = desired.subtract(current);
            if (toDesired.lengthSquared() < 1.0E-6D) {
                setVelocity(getVelocity().scale(IDLE_DAMPING));
                setState(SummonState.FOLLOW_OWNER);
                return;
            }

            double speed = Math.min(getSpec().getMoveSpeed() + Math.sqrt(toDesired.lengthSquared()) * 0.04D,
                    getSpec().getMoveSpeed() * 2.0D);
            Vec3d desiredVelocity = toDesired.normalize().scale(speed);
            Vec3d currentVelocity = getVelocity();
            double accel = getSpec().getAcceleration();
            setVelocity(currentVelocity.scale(1.0D - accel).add(desiredVelocity.scale(accel)));
            setState(SummonState.FOLLOW_OWNER);
        }

        private Vec3d computeApproachPoint(Vec3d targetCenter, Vec3d ownerCenter, double offset) {
            Vec3d fromOwner = targetCenter.subtract(ownerCenter);
            if (fromOwner.lengthSquared() < 1.0E-6D) {
                fromOwner = new Vec3d(1, 0, 0);
            }
            Vec3d lateral = new Vec3d(-fromOwner.z, 0, fromOwner.x);
            if (lateral.lengthSquared() < 1.0E-6D) {
                lateral = new Vec3d(1, 0, 0);
            }
            lateral = lateral.normalize();
            return targetCenter.add(lateral.scale(offset)).add(0.0D, 0.12D, 0.0D);
        }

        private void resetCombatState() {
            phase = Phase.APPROACH;
            phaseTicks = 0;
            trackedTargetId = -1;
            penetrateTicksLeft = 0;
            reacquireTicksLeft = 0;
            dashDirection = new Vec3d(0, 0, 1);
        }

        private enum Phase {
            APPROACH,
            DASHING,
            PENETRATING,
            REACQUIRING
        }
    }
}
