package com.smd.bulletapi.common.summon.type.impl;

import com.smd.bulletapi.api.LaserApi;
import com.smd.bulletapi.api.builder.LaserBuilder;
import com.smd.bulletapi.api.summon.AbstractSummonEntity;
import com.smd.bulletapi.api.summon.SummonSpec;
import com.smd.bulletapi.api.summon.SummonType;
import com.smd.bulletapi.common.AttackSourceInfo;
import com.smd.bulletapi.common.DanmakuManager;
import com.smd.bulletapi.common.summon.SummonPresetKeys;
import com.smd.bulletapi.common.summon.SummonState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LaserEyeType extends SummonType {
    public LaserEyeType() {
        super(SummonPresetKeys.LASER_EYE, createSpec());
    }

    private static SummonSpec createSpec() {
        return new SummonSpec()
                .life(6000)
                .damage(3.0f)
                .slotCost(2)
                .size(0.9f)
                .rendererType("model_json")
                .followRange(28.0D)
                .attackRange(24.0D)
                .leashRange(32.0D)
                .moveSpeed(0.24D)
                .acceleration(0.13D)
                .idleHeight(1.55D)
                .idleRadius(2.2D)
                .retargetIntervalTicks(8)
                .syncIntervalTicks(1)
                .bodyCollisionIntervalTicks(0)
                .collisionShape(null)
                .set("scale", 0.42f)
                .set("tint", 0xA8F2FF)
                .set("model", "minecraft:beacon")
                .set("variant", "inventory")
                .set("rot_mode", "face_camera");
    }

    @Override
    public AbstractSummonEntity createEntity(int id, World world, EntityLivingBase owner,
                                             Vec3d position, int formationIndex, long spawnTick) {
        return new LaserEyeEntity(id, this, owner, position, formationIndex, spawnTick);
    }

    private static final class LaserEyeEntity extends AbstractSummonEntity {
        private LaserEyeEntity(int id, SummonType type, EntityLivingBase owner, Vec3d position,
                               int formationIndex, long spawnTick) {
            super(id, type, owner, position, formationIndex, spawnTick);
        }

        @Override
        public void tickServer(World world, EntityLivingBase owner, EntityLivingBase currentTarget) {
            if (owner == null || owner.isDead) {
                stopActiveLaser(world);
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
                stopActiveLaser(world);
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
            setState(SummonState.CHASING_TARGET);
            moveToward(desiredPosition, getSpec().getMoveSpeed(), 2.0D, 0.65D);

            Vec3d start = getPosition();
            double attackRangeSq = getSpec().getAttackRange() * getSpec().getAttackRange();
            if (target.getDistanceSq(start.x, start.y, start.z) > attackRangeSq) {
                stopActiveLaser(world);
                setState(SummonState.IDLE);
                return;
            }

            Vec3d direction = targetCenter.subtract(start);
            if (direction.lengthSquared() < 1.0E-6D) {
                stopActiveLaser(world);
                setState(SummonState.IDLE);
                return;
            }

            Vec3d normalizedDirection = direction.normalize();
            if (hasActiveLaser()) {
                boolean updated = DanmakuManager.getInstance().updateLaserTransform(world, getActiveLaserId(), start, normalizedDirection);
                if (updated) {
                    setState(SummonState.ATTACKING);
                    return;
                }
                clearActiveLaserId();
            }

            if (!canAttack()) {
                return;
            }

            LaserBuilder builder = LaserApi.builder(world)
                    .start(start)
                    .direction(normalizedDirection)
                    .followShooter(false)
                    .maxLength(32.0D)
                    .thickness(0.8f)
                    .damage(3.0f)
                    .color(0x66CCFF)
                    .rendererType("laser_poly")
                    .penetrate(true)
                    .blockStops(true)
                    .eventIntervalTicks(5)
                    .life(-1)
                    .shooter(owner)
                    .attackSourceInfo(AttackSourceInfo.summonChildLaser(owner.getUniqueID(), getId(), getDefinitionId()))
                    .hitBehavior(ctx -> {
                        ctx.damage = ctx.laser.getDamage();
                        ctx.world.playSound(null, ctx.hitEntity.posX, ctx.hitEntity.posY, ctx.hitEntity.posZ,
                                SoundEvents.BLOCK_NOTE_HARP, SoundCategory.PLAYERS, 0.4F, 1.8F);
                    });

            builder.set("alpha", 0.8f)
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
                    .set("deco_color", 0x88CCFF);

            int laserId = builder.spawn();
            setActiveLaserId(laserId);
            setAttackCooldown(18);
            setState(SummonState.ATTACKING);
        }

        private void stopActiveLaser(World world) {
            if (!hasActiveLaser()) {
                return;
            }
            LaserApi.handle(world, getActiveLaserId()).remove();
            clearActiveLaserId();
        }
    }
}
