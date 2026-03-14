package com.smd.bulletapi.common;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.runtime.IBulletActor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@PublicApi
public class CollisionContext {
    public final IBulletActor bullet;
    public final World world;
    public final Entity hitEntity;
    public final EntityLivingBase shooter;
    public final ItemStack shooterHeldItem;
    public final AttackSourceInfo attackSource;
    public float damage;        // 可修改
    public boolean canceled;    // 是否取消默认处理

    public CollisionContext(IBulletActor bullet, World world, Entity hitEntity) {
        this.bullet = bullet;
        this.world = world;
        this.hitEntity = hitEntity;
        this.damage = bullet.getDamage();
        this.canceled = false;
        this.shooter = bullet.getShooter();
        ItemStack stack = bullet.getShooterHeldItem();
        this.shooterHeldItem = stack == null ? null : stack.copy();
        this.attackSource = bullet.getAttackSourceInfo();
    }

    public boolean isSummonSource() { return attackSource != null && attackSource.isSummonSource(); }
    public boolean isSummonBody() { return attackSource != null && attackSource.isSummonBody(); }
    public boolean isSummonChildBullet() { return attackSource != null && attackSource.isSummonChildBullet(); }
    public boolean isSummonChildLaser() { return attackSource != null && attackSource.isSummonChildLaser(); }
}
