package com.smd.bulletapi.common.summon.type.impl;

import com.smd.bulletapi.api.BulletApi;
import com.smd.bulletapi.api.summon.AbstractSummonEntity;
import com.smd.bulletapi.api.summon.SummonSpec;
import com.smd.bulletapi.api.summon.SummonType;
import com.smd.bulletapi.common.AttackSourceInfo;
import com.smd.bulletapi.common.collision.SphereShape;
import com.smd.bulletapi.common.summon.SummonPresetKeys;
import com.smd.bulletapi.common.summon.SummonState;
import com.smd.bulletapi.common.summon.SummonTargetSource;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class FairyOrbType extends SummonType {
    public FairyOrbType() {
        super(SummonPresetKeys.FAIRY_ORB, createSpec());
    }

    private static SummonSpec createSpec() {
        return new SummonSpec()
                .life(6000)
                .damage(2.5f)
                .slotCost(1)
                .texture("bulletapi:textures/entity/bullet.png")
                .color(0x7FE7FF)
                .size(0.85f)
                .rendererType("billboard")
                .followRange(24.0D)
                .attackRange(18.0D)
                .leashRange(28.0D)
                .moveSpeed(0.28D)
                .acceleration(0.14D)
                .idleHeight(1.35D)
                .idleRadius(1.9D)
                .retargetIntervalTicks(10)
                .syncIntervalTicks(2)
                .bodyCollisionIntervalTicks(8)
                .collisionShape(new SphereShape(0.65D))
                .set("scale", 0.85f);
    }

    @Override
    public AbstractSummonEntity createEntity(int id, World world, EntityLivingBase owner,
                                             Vec3d position, int formationIndex, long spawnTick) {
        return new FairyOrbEntity(id, this, owner, position, formationIndex, spawnTick);
    }

    private static final class FairyOrbEntity extends AbstractSummonEntity {
        private static final int COOLDOWN_TICKS = 16;
        private static final int BULLET_LIFE = 52;
        private static final double BULLET_SPEED = 0.72D;

        private FairyOrbEntity(int id, SummonType type, EntityLivingBase owner, Vec3d position,
                               int formationIndex, long spawnTick) {
            super(id, type, owner, position, formationIndex, spawnTick);
        }

        @Override
        public void tickServer(World world, EntityLivingBase owner, EntityLivingBase currentTarget) {
            if (owner == null || owner.isDead) {
                markDead();
                return;
            }

            EntityLivingBase target = currentTarget;
            if (target == null && shouldRetarget()) {
                target = acquireTarget(world, owner);
                resetRetargetCooldown();
            }

            if (target == null || target.isDead || isOutsideLeash(owner)) {
                clearTarget();
                followOwnerOrbit(owner);
                return;
            }

            double angle = (world.getTotalWorldTime() * 0.14D) + getFormationIndex();
            Vec3d targetCenter = getTargetCenter(target);
            double radius = Math.max(1.2D, getSpec().getIdleRadius() * 0.85D);
            Vec3d desiredPosition = targetCenter.add(
                    Math.cos(angle) * radius,
                    0.8D + Math.sin(angle * 0.5D) * 0.25D,
                    Math.sin(angle) * radius
            );
            setTarget(target, SummonTargetSource.AUTO);
            setState(SummonState.CHASING_TARGET);
            moveToward(desiredPosition, getSpec().getMoveSpeed(), 2.0D, 0.65D);

            if (!canAttack()) {
                return;
            }
            Vec3d spawnPos = getPosition();
            if (target.getDistanceSq(spawnPos.x, spawnPos.y, spawnPos.z) > getSpec().getAttackRange() * getSpec().getAttackRange()) {
                return;
            }
            Vec3d direction = getTargetCenter(target).subtract(spawnPos);
            if (direction.lengthSquared() < 1.0E-6D) {
                return;
            }

            BulletApi.builder(world)
                    .position(spawnPos)
                    .velocity(direction.normalize().scale(BULLET_SPEED))
                    .life(BULLET_LIFE)
                    .damage(2.25f)
                    .texture("bulletapi:textures/entity/bullet.png")
                    .color(0xCCFFFF)
                    .size(0.42f)
                    .rendererType("billboard")
                    .collisionShape(new SphereShape(0.4D))
                    .hitBehavior(ctx -> BulletApi.handle(ctx.world, ctx.bullet.getId()).remove())
                    .shooter(owner)
                    .attackSourceInfo(AttackSourceInfo.summonChildBullet(owner.getUniqueID(), getId(), getDefinitionId()))
                    .spawn();

            setAttackCooldown(COOLDOWN_TICKS);
            setState(SummonState.ATTACKING);
        }
    }
}
