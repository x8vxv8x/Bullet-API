package com.smd.bulletapi.client;

import com.smd.bulletapi.client.render.IBulletRenderer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class ClientBullet {
    // 原有字段
    private final int id;
    private Vec3d position;
    private Vec3d prevPosition;
    private Vec3d velocity;
    private int life;
    private final int maxLife;
    private float damage;
    private boolean dead;
    private ResourceLocation texture;          // 纹理（可为空）
    private NBTTagCompound customData;

    // 新增：渲染器（由外部设置）
    private IBulletRenderer renderer;

    // 新增：模型变换属性（缓存，避免每帧解析 NBT）
    private float rotationYaw;      // 偏航角（度）
    private float rotationPitch;    // 俯仰角（度）
    private float scale;            // 缩放系数

    // 原有缓存字段（颜色、尺寸）
    private int cachedColor;        // 默认白色 0xFFFFFF
    private float cachedSize;        // 默认 0.5f

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

        // 从 customData 初始化所有缓存字段
        updateCacheFromCustomData();
    }

    /** 从 customData 更新所有缓存字段（在 customData 变化时调用） */
    private void updateCacheFromCustomData() {
        // 颜色（默认白色）
        this.cachedColor = customData.hasKey("Color") ? customData.getInteger("Color") : 0xFFFFFF;

        // 尺寸（默认 0.5）
        this.cachedSize = customData.hasKey("Size") ? customData.getFloat("Size") : 0.5f;

        // 旋转角度（默认 0）
        this.rotationYaw = customData.hasKey("RotationYaw") ? customData.getFloat("RotationYaw") : 0.0f;
        this.rotationPitch = customData.hasKey("RotationPitch") ? customData.getFloat("RotationPitch") : 0.0f;

        // 缩放（默认 1.0）
        this.scale = customData.hasKey("Scale") ? customData.getFloat("Scale") : 1.0f;
    }

    /** 每 tick 调用：更新位置、生命 */
    public void tick() {
        if (dead) return;
        prevPosition = position;
        position = position.add(velocity);
        life--;
        if (life <= 0) dead = true;
    }

    /** 获取插值位置（返回新 Vec3d，兼容旧代码） */
    public Vec3d getRenderPosition(float partialTicks) {
        double x = prevPosition.x + (position.x - prevPosition.x) * partialTicks;
        double y = prevPosition.y + (position.y - prevPosition.y) * partialTicks;
        double z = prevPosition.z + (position.z - prevPosition.z) * partialTicks;
        return new Vec3d(x, y, z);
    }

    /** 高效获取插值位置，写入给定数组（推荐渲染循环使用） */
    public void getRenderPosition(float partialTicks, double[] outPos) {
        outPos[0] = prevPosition.x + (position.x - prevPosition.x) * partialTicks;
        outPos[1] = prevPosition.y + (position.y - prevPosition.y) * partialTicks;
        outPos[2] = prevPosition.z + (position.z - prevPosition.z) * partialTicks;
    }

    // ========== Getter / Setter ==========

    public int getId() { return id; }
    public boolean isDead() { return dead; }
    public void setVelocity(Vec3d velocity) { this.velocity = velocity; }
    public Vec3d getVelocity() { return velocity; }
    public ResourceLocation getTexture() { return texture; }
    public NBTTagCompound getCustomData() { return customData; }

    // 颜色和尺寸（直接返回缓存）
    public int getColor() { return cachedColor; }
    public float getSize() { return cachedSize; }

    // 新增的变换属性
    public float getRotationYaw() { return rotationYaw; }
    public float getRotationPitch() { return rotationPitch; }
    public float getScale() { return scale; }

    // 渲染器相关
    public IBulletRenderer getRenderer() { return renderer; }
    public void setRenderer(IBulletRenderer renderer) { this.renderer = renderer; }

    /** 更新自定义数据并重新计算缓存（用于网络包更新） */
    public void setCustomData(NBTTagCompound newData) {
        this.customData = newData == null ? new NBTTagCompound() : newData;
        updateCacheFromCustomData();
    }
}