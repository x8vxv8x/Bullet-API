package com.smd.bulletapi.common.summon.behavior.impl;

import com.smd.bulletapi.BulletAPI;
import com.smd.bulletapi.common.AttackSourceInfo;
import com.smd.bulletapi.common.DanmakuManager;
import com.smd.bulletapi.common.LaserCollisionContext;
import com.smd.bulletapi.common.summon.SummonContext;
import com.smd.bulletapi.common.summon.SummonState;
import com.smd.bulletapi.common.summon.behavior.ISummonAttackPattern;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;

import java.util.function.Consumer;

public class ShootLaserPattern implements ISummonAttackPattern {
    private final int cooldownTicks;
    private final int laserLife;
    private final double maxLength;
    private final float thickness;
    private final float damage;
    private final int color;
    private final String rendererType;
    private final int eventIntervalTicks;
    private final boolean penetrate;
    private final boolean blockStops;
    private final Consumer<LaserCollisionContext> onCollision;
    private final Consumer<BulletAPI.LaserBuilder> configureBuilder;

    public ShootLaserPattern(int cooldownTicks, int laserLife, double maxLength, float thickness,
                             float damage, int color, String rendererType, int eventIntervalTicks,
                             boolean penetrate, boolean blockStops,
                             Consumer<LaserCollisionContext> onCollision) {
        this(cooldownTicks, laserLife, maxLength, thickness, damage, color, rendererType,
                eventIntervalTicks, penetrate, blockStops, onCollision, null);
    }

    public ShootLaserPattern(int cooldownTicks, int laserLife, double maxLength, float thickness,
                             float damage, int color, String rendererType, int eventIntervalTicks,
                             boolean penetrate, boolean blockStops,
                             Consumer<LaserCollisionContext> onCollision,
                             Consumer<BulletAPI.LaserBuilder> configureBuilder) {
        this.cooldownTicks = cooldownTicks;
        this.laserLife = laserLife;
        this.maxLength = maxLength;
        this.thickness = thickness;
        this.damage = damage;
        this.color = color;
        this.rendererType = rendererType;
        this.eventIntervalTicks = eventIntervalTicks;
        this.penetrate = penetrate;
        this.blockStops = blockStops;
        this.onCollision = onCollision;
        this.configureBuilder = configureBuilder;
    }

    @Override
    public void tickAttack(SummonContext context) {
        EntityLivingBase target = context.getTarget();
        if (target == null || target.isDead) {
            stopActiveLaser(context);
            context.summon.setState(SummonState.IDLE);
            return;
        }

        Vec3d start = context.summon.getPosition();
        double attackRangeSq = context.definition.getAttackRange() * context.definition.getAttackRange();
        if (target.getDistanceSq(start.x, start.y, start.z) > attackRangeSq) {
            stopActiveLaser(context);
            context.summon.setState(SummonState.IDLE);
            return;
        }

        Vec3d targetPos = context.getTargetCenter();
        Vec3d direction = targetPos.subtract(start);
        if (direction.lengthSquared() < 1.0E-6) {
            stopActiveLaser(context);
            context.summon.setState(SummonState.IDLE);
            return;
        }

        Vec3d normalizedDirection = direction.normalize();
        if (context.summon.hasActiveLaser()) {
            boolean updated = DanmakuManager.getInstance().updateLaserTransform(
                    context.world,
                    context.summon.getActiveLaserId(),
                    start,
                    normalizedDirection
            );
            if (!updated) {
                context.summon.clearActiveLaserId();
            } else {
                context.summon.setState(SummonState.ATTACKING);
                return;
            }
        }

        if (!context.summon.canAttack()) return;

        BulletAPI.LaserBuilder builder = BulletAPI.laser(context.world)
                .start(start)
                .direction(normalizedDirection)
                .followShooter(false)
                .maxLength(maxLength)
                .thickness(thickness)
                .damage(damage)
                .color(color)
                .rendererType(rendererType)
                .penetrate(penetrate)
                .blockStops(blockStops)
                .eventIntervalTicks(eventIntervalTicks)
                .life(laserLife)
                .shooter(context.owner)
                .attackSourceInfo(AttackSourceInfo.summonChildLaser(
                        context.owner.getUniqueID(),
                        context.summon.getId(),
                        context.summon.getDefinitionId()
                ))
                .onCollision(onCollision);

        if (configureBuilder != null) {
            configureBuilder.accept(builder);
        }

        int laserId = builder.spawn();
        context.summon.setActiveLaserId(laserId);

        context.summon.setAttackCooldown(cooldownTicks);
        context.summon.setState(SummonState.ATTACKING);
    }

    private void stopActiveLaser(SummonContext context) {
        if (!context.summon.hasActiveLaser()) return;
        BulletAPI.removeLaser(context.world, context.summon.getActiveLaserId());
        context.summon.clearActiveLaserId();
    }

    public static Consumer<LaserCollisionContext> soundOnHit(SoundEvent sound, float volume, float pitch) {
        return ctx -> {
            ctx.damage = ctx.laser.getDamage();
            if (sound != null) {
                ctx.world.playSound(null, ctx.hitEntity.posX, ctx.hitEntity.posY, ctx.hitEntity.posZ,
                        sound, SoundCategory.PLAYERS, volume, pitch);
            }
        };
    }
}
