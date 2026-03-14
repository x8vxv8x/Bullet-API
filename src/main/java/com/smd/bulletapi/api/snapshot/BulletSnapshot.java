package com.smd.bulletapi.api.snapshot;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.common.AttackSourceInfo;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

@PublicApi
public final class BulletSnapshot {
    private final int id;
    private final Vec3d position;
    private final Vec3d velocity;
    private final int life;
    private final float damage;
    private final String texture;
    private final int color;
    private final float size;
    private final String rendererType;
    private final NBTTagCompound customData;
    private final boolean onlyPlayer;
    private final AttackSourceInfo attackSourceInfo;

    public BulletSnapshot(int id, Vec3d position, Vec3d velocity, int life, float damage,
                          String texture, int color, float size, String rendererType,
                          NBTTagCompound customData, boolean onlyPlayer,
                          AttackSourceInfo attackSourceInfo) {
        this.id = id;
        this.position = position;
        this.velocity = velocity;
        this.life = life;
        this.damage = damage;
        this.texture = texture;
        this.color = color;
        this.size = size;
        this.rendererType = rendererType;
        this.customData = customData == null ? new NBTTagCompound() : customData.copy();
        this.onlyPlayer = onlyPlayer;
        this.attackSourceInfo = attackSourceInfo;
    }

    public int getId() { return id; }
    public Vec3d getPosition() { return position; }
    public Vec3d getVelocity() { return velocity; }
    public int getLife() { return life; }
    public float getDamage() { return damage; }
    public String getTexture() { return texture; }
    public int getColor() { return color; }
    public float getSize() { return size; }
    public String getRendererType() { return rendererType; }
    public NBTTagCompound getCustomData() { return customData.copy(); }
    public boolean isOnlyPlayer() { return onlyPlayer; }
    public AttackSourceInfo getAttackSourceInfo() { return attackSourceInfo; }
}
