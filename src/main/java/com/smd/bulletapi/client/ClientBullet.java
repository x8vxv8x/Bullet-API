package com.smd.bulletapi.client;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class ClientBullet {
    private final int id;
    private Vec3d position;      // 当前tick位置
    private Vec3d prevPosition;  // 上一tick位置（用于插值）
    private Vec3d velocity;      // 速度（每tick移动量）
    private int life;
    private final int maxLife;
    private float damage;
    private boolean dead;
    private ResourceLocation texture;      // 客户端纹理对象
    private NBTTagCompound customData;

    public ClientBullet(int id, Vec3d position, Vec3d velocity, int maxLife, float damage,
                        ResourceLocation texture, NBTTagCompound customData) {
        this.id = id;
        this.position = position;
        this.prevPosition = position;
        this.velocity = velocity;
        this.maxLife = maxLife;
        this.life = maxLife;
        this.damage = damage;
        this.dead = false;
        this.texture = texture;
        this.customData = customData == null ? new NBTTagCompound() : customData;
    }

    /** 每tick调用：保存上一位置，根据速度移动，减少生命 */
    public void tick() {
        if (dead) return;
        prevPosition = position;
        position = position.add(velocity);
        life--;
        if (life <= 0) dead = true;
    }

    /** 获取渲染位置（线性插值）- 返回新Vec3d（保留原方法兼容旧代码） */
    public Vec3d getRenderPosition(float partialTicks) {
        double x = prevPosition.x + (position.x - prevPosition.x) * partialTicks;
        double y = prevPosition.y + (position.y - prevPosition.y) * partialTicks;
        double z = prevPosition.z + (position.z - prevPosition.z) * partialTicks;
        return new Vec3d(x, y, z);
    }

    /** 高效获取渲染位置，将结果写入给定数组（推荐用于渲染循环） */
    public void getRenderPosition(float partialTicks, double[] outPos) {
        outPos[0] = prevPosition.x + (position.x - prevPosition.x) * partialTicks;
        outPos[1] = prevPosition.y + (position.y - prevPosition.y) * partialTicks;
        outPos[2] = prevPosition.z + (position.z - prevPosition.z) * partialTicks;
    }

    public int getId() {
        return id; }
    public boolean isDead() {
        return dead; }
    public void setVelocity(Vec3d velocity) {
        this.velocity = velocity; }
    public Vec3d getVelocity() {
        return velocity; }
    public ResourceLocation getTexture() {
        return texture; }
    public NBTTagCompound getCustomData() {
        return customData; }
}