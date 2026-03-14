package com.smd.bulletapi.common.summon.behavior.impl;

import com.smd.bulletapi.api.BulletApi;
import com.smd.bulletapi.common.AttackSourceInfo;
import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.collision.SphereShape;
import com.smd.bulletapi.common.summon.SummonContext;
import com.smd.bulletapi.common.summon.SummonState;
import com.smd.bulletapi.common.summon.behavior.ISummonAttackPattern;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;

public class ShootBulletPattern implements ISummonAttackPattern {
    private final int cooldownTicks;
    private final int bulletLife;
    private final double bulletSpeed;
    private final float bulletDamage;
    private final String bulletTexture;
    private final int bulletColor;
    private final float bulletSize;
    private final String bulletRendererType;

    public ShootBulletPattern(int cooldownTicks, int bulletLife, double bulletSpeed, float bulletDamage,
                              String bulletTexture, int bulletColor, float bulletSize, String bulletRendererType) {
        this.cooldownTicks = cooldownTicks;
        this.bulletLife = bulletLife;
        this.bulletSpeed = bulletSpeed;
        this.bulletDamage = bulletDamage;
        this.bulletTexture = bulletTexture;
        this.bulletColor = bulletColor;
        this.bulletSize = bulletSize;
        this.bulletRendererType = bulletRendererType;
    }

    @Override
    public void tickAttack(SummonContext context) {
        EntityLivingBase target = context.getTarget();
        if (target == null || target.isDead) return;
        if (!context.summon.canAttack()) return;
        Vec3d spawnPos = context.summon.getPosition();
        if (target.getDistanceSq(spawnPos.x, spawnPos.y, spawnPos.z) > context.definition.getAttackRange() * context.definition.getAttackRange()) return;

        Vec3d targetPos = context.getTargetCenter();
        Vec3d direction = targetPos.subtract(spawnPos);
        if (direction.lengthSquared() < 1.0E-6) return;

        BulletApi.builder(context.world)
                .position(spawnPos)
                .velocity(direction.normalize().scale(bulletSpeed))
                .life(bulletLife)
                .damage(bulletDamage)
                .texture(bulletTexture)
                .color(bulletColor)
                .size(bulletSize)
                .rendererType(bulletRendererType)
                .collisionShape(new SphereShape(0.4))
                .onCollision(this::onBulletCollision)
                .shooter(context.owner)
                .attackSourceInfo(AttackSourceInfo.summonChildBullet(
                        context.owner.getUniqueID(),
                        context.summon.getId(),
                        context.summon.getDefinitionId()
                ))
                .spawn();

        context.summon.setAttackCooldown(cooldownTicks);
        context.summon.setState(SummonState.ATTACKING);
    }

    private void onBulletCollision(CollisionContext ctx) {
        BulletApi.remove(ctx.world, ctx.bullet.getId());
    }
}
