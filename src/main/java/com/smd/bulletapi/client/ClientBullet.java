package com.smd.bulletapi.client;

import com.smd.bulletapi.client.render.IBulletRenderer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class ClientBullet {
    private final int id;
    private double positionX;
    private double positionY;
    private double positionZ;
    private double prevPositionX;
    private double prevPositionY;
    private double prevPositionZ;
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private int life;
    private final int maxLife;
    private float damage;
    private boolean dead;
    private ResourceLocation texture;        // 纹理位置（可为null）
    private int color;                        // 颜色 RGB
    private float size;                        // 尺寸
    private String rendererType;                // 渲染器类型
    private NBTTagCompound customData;          // 其他自定义数据
    private IBulletRenderer renderer;           // 渲染器实例

    public ClientBullet(int id, Vec3d position, Vec3d velocity, int maxLife, float damage,
                        ResourceLocation texture, int color, float size, String rendererType,
                        NBTTagCompound customData) {
        this.id = id;
        this.positionX = position.x;
        this.positionY = position.y;
        this.positionZ = position.z;
        this.prevPositionX = position.x;
        this.prevPositionY = position.y;
        this.prevPositionZ = position.z;
        this.velocityX = velocity.x;
        this.velocityY = velocity.y;
        this.velocityZ = velocity.z;
        this.maxLife = maxLife;
        this.life = maxLife;
        this.damage = damage;
        this.dead = false;
        this.texture = texture;
        this.color = color;
        this.size = size;
        this.rendererType = rendererType;
        this.customData = customData == null ? new NBTTagCompound() : customData;
    }

    public void tick() {
        if (dead) return;
        prevPositionX = positionX;
        prevPositionY = positionY;
        prevPositionZ = positionZ;
        positionX += velocityX;
        positionY += velocityY;
        positionZ += velocityZ;
        life--;
        if (life <= 0) dead = true;
    }

    public Vec3d getRenderPosition(float partialTicks) {
        return new Vec3d(getRenderX(partialTicks), getRenderY(partialTicks), getRenderZ(partialTicks));
    }

    public void getRenderPosition(float partialTicks, double[] outPos) {
        outPos[0] = getRenderX(partialTicks);
        outPos[1] = getRenderY(partialTicks);
        outPos[2] = getRenderZ(partialTicks);
    }

    public double getRenderX(float partialTicks) {
        return prevPositionX + (positionX - prevPositionX) * partialTicks;
    }

    public double getRenderY(float partialTicks) {
        return prevPositionY + (positionY - prevPositionY) * partialTicks;
    }

    public double getRenderZ(float partialTicks) {
        return prevPositionZ + (positionZ - prevPositionZ) * partialTicks;
    }

    public int getId() { return id; }
    public boolean isDead() { return dead; }
    public void setVelocity(Vec3d velocity) {
        this.velocityX = velocity.x;
        this.velocityY = velocity.y;
        this.velocityZ = velocity.z;
    }
    public Vec3d getVelocity() { return new Vec3d(velocityX, velocityY, velocityZ); }
    public ResourceLocation getTexture() { return texture; }
    public int getColor() { return color; }
    public float getSize() { return size; }
    public String getRendererType() { return rendererType; }
    public NBTTagCompound getCustomData() { return customData; }
    public IBulletRenderer getRenderer() { return renderer; }
    public void setRenderer(IBulletRenderer renderer) { this.renderer = renderer; }
}
