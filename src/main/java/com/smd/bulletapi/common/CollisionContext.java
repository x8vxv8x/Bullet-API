package com.smd.bulletapi.common;

import com.smd.bulletapi.server.Bullet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class CollisionContext {
    public final Bullet bullet;
    public final World world;
    public final Entity hitEntity;
    public final EntityLivingBase shooter;
    public final ItemStack shooterHeldItem;
    public float damage;        // 可修改
    public boolean canceled;    // 是否取消默认处理

    public CollisionContext(Bullet bullet, World world, Entity hitEntity) {
        this.bullet = bullet;
        this.world = world;
        this.hitEntity = hitEntity;
        this.damage = bullet.getDamage();
        this.canceled = false;
        this.shooter = bullet.getShooter();
        ItemStack stack = bullet.getShooterHeldItem();
        this.shooterHeldItem = stack == null ? null : stack.copy();
    }
}
