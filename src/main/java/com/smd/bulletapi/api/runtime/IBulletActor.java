package com.smd.bulletapi.api.runtime;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.common.AttackSourceInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

@PublicApi
public interface IBulletActor {
    int getId();
    Vec3d getPosition();
    Vec3d getVelocity();
    void setVelocity(Vec3d velocity);
    void setVelocity(double x, double y, double z);
    void setPosition(Vec3d position);
    void setPosition(double x, double y, double z);
    void setLife(int life);
    void markDead();
    int getLife();
    float getDamage();
    boolean isDead();
    String getTexture();
    int getColor();
    float getSize();
    String getRendererType();
    NBTTagCompound getCustomData();
    void setCustomData(NBTTagCompound customData);
    boolean isOnlyPlayer();
    EntityLivingBase getShooter();
    ItemStack getShooterHeldItem();
    AttackSourceInfo getAttackSourceInfo();
}
