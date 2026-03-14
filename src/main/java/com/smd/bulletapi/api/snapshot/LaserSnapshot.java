package com.smd.bulletapi.api.snapshot;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.common.AttackSourceInfo;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

@PublicApi
public final class LaserSnapshot {
    private final int id;
    private final Vec3d start;
    private final Vec3d direction;
    private final double maxLength;
    private final double currentLength;
    private final float thickness;
    private final int life;
    private final float damage;
    private final int color;
    private final String rendererType;
    private final NBTTagCompound customData;
    private final boolean onlyPlayer;
    private final boolean penetrate;
    private final boolean blockStops;
    private final AttackSourceInfo attackSourceInfo;

    public LaserSnapshot(int id, Vec3d start, Vec3d direction, double maxLength, double currentLength,
                         float thickness, int life, float damage, int color, String rendererType,
                         NBTTagCompound customData, boolean onlyPlayer, boolean penetrate,
                         boolean blockStops, AttackSourceInfo attackSourceInfo) {
        this.id = id;
        this.start = start;
        this.direction = direction;
        this.maxLength = maxLength;
        this.currentLength = currentLength;
        this.thickness = thickness;
        this.life = life;
        this.damage = damage;
        this.color = color;
        this.rendererType = rendererType;
        this.customData = customData == null ? new NBTTagCompound() : customData.copy();
        this.onlyPlayer = onlyPlayer;
        this.penetrate = penetrate;
        this.blockStops = blockStops;
        this.attackSourceInfo = attackSourceInfo;
    }

    public int getId() { return id; }
    public Vec3d getStart() { return start; }
    public Vec3d getDirection() { return direction; }
    public double getMaxLength() { return maxLength; }
    public double getCurrentLength() { return currentLength; }
    public float getThickness() { return thickness; }
    public int getLife() { return life; }
    public float getDamage() { return damage; }
    public int getColor() { return color; }
    public String getRendererType() { return rendererType; }
    public NBTTagCompound getCustomData() { return customData.copy(); }
    public boolean isOnlyPlayer() { return onlyPlayer; }
    public boolean isPenetrate() { return penetrate; }
    public boolean isBlockStops() { return blockStops; }
    public AttackSourceInfo getAttackSourceInfo() { return attackSourceInfo; }
}
