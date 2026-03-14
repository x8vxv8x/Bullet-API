package com.smd.bulletapi.api.runtime;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.common.AttackSourceInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

@PublicApi
public interface ILaserActor {
    int getId();
    Vec3d getStart();
    void setStart(Vec3d start);
    Vec3d getDirection();
    void setDirection(Vec3d direction);
    double getMaxLength();
    double getCurrentLength();
    void setCurrentLength(double length);
    float getThickness();
    float getDamage();
    boolean isDead();
    boolean isPenetrate();
    boolean isFollowShooter();
    boolean isOnlyPlayer();
    boolean isBlockStops();
    int getEventIntervalTicks();
    int getColor();
    String getRendererType();
    NBTTagCompound getCustomData();
    EntityLivingBase getShooter();
    ItemStack getShooterHeldItem();
    AttackSourceInfo getAttackSourceInfo();
    void markDead();
    int getLife();
}
