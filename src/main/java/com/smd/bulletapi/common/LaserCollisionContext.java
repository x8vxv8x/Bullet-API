package com.smd.bulletapi.common;

import com.smd.bulletapi.server.Laser;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class LaserCollisionContext {
    public final Laser laser;
    public final World world;
    public final EntityLivingBase hitEntity;
    public final EntityLivingBase shooter;
    public final ItemStack shooterHeldItem;
    public final AttackSourceInfo attackSource;
    public float damage;
    public boolean canceled;

    public LaserCollisionContext(Laser laser, World world, EntityLivingBase hitEntity) {
        this.laser = laser;
        this.world = world;
        this.hitEntity = hitEntity;
        this.damage = laser.getDamage();
        this.canceled = false;
        this.shooter = laser.getShooter();
        ItemStack stack = laser.getShooterHeldItem();
        this.shooterHeldItem = stack == null ? null : stack.copy();
        this.attackSource = laser.getAttackSourceInfo();
    }

    public boolean isSummonSource() { return attackSource != null && attackSource.isSummonSource(); }
    public boolean isSummonChildLaser() { return attackSource != null && attackSource.isSummonChildLaser(); }
}
