package com.smd.bulletapi.client;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.client.render.IBulletRenderer;
import com.smd.bulletapi.common.RenderStateData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

@InternalApi
public class ClientBullet {
    protected final int id;
    protected double positionX;
    protected double positionY;
    protected double positionZ;
    protected double prevPositionX;
    protected double prevPositionY;
    protected double prevPositionZ;
    protected double velocityX;
    protected double velocityY;
    protected double velocityZ;
    protected int life;
    protected final int maxLife;
    protected float damage;
    protected boolean dead;
    protected ResourceLocation texture;        // 纹理位置（可为null）
    protected int color;                        // 颜色 RGB
    protected float size;                        // 尺寸
    protected String rendererType;                // 渲染器类型
    protected NBTTagCompound customData;          // 其他自定义数据
    protected IBulletRenderer renderer;           // 渲染器实例

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
        if (dead) {
            return;
        }
        prevPositionX = positionX;
        prevPositionY = positionY;
        prevPositionZ = positionZ;
        positionX += velocityX;
        positionY += velocityY;
        positionZ += velocityZ;
        life--;
        if (life <= 0) {
            dead = true;
        }
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
    public void applySnapshot(Vec3d position, Vec3d velocity, int life) {
        this.prevPositionX = this.positionX;
        this.prevPositionY = this.positionY;
        this.prevPositionZ = this.positionZ;
        this.positionX = position.x;
        this.positionY = position.y;
        this.positionZ = position.z;
        this.velocityX = velocity.x;
        this.velocityY = velocity.y;
        this.velocityZ = velocity.z;
        this.life = life;
        this.dead = life <= 0;
    }
    public void applyUpdate(Vec3d position, Vec3d velocity, Integer life) {
        if (position != null) {
            this.prevPositionX = this.positionX;
            this.prevPositionY = this.positionY;
            this.prevPositionZ = this.positionZ;
            this.positionX = position.x;
            this.positionY = position.y;
            this.positionZ = position.z;
        }
        if (velocity != null) {
            this.velocityX = velocity.x;
            this.velocityY = velocity.y;
            this.velocityZ = velocity.z;
        }
        if (life != null) {
            this.life = life;
            this.dead = life <= 0;
        }
    }
    public void setVelocity(Vec3d velocity) {
        this.velocityX = velocity.x;
        this.velocityY = velocity.y;
        this.velocityZ = velocity.z;
    }
    public Vec3d getVelocity() { return new Vec3d(velocityX, velocityY, velocityZ); }
    public ResourceLocation getTexture() { return texture; }
    public void setTexture(ResourceLocation texture) { this.texture = texture; }
    public int getColor() { return color; }
    public float getSize() { return size; }
    public String getRendererType() { return rendererType; }
    public void setRendererType(String rendererType) { this.rendererType = rendererType == null || rendererType.isEmpty() ? null : rendererType; }
    public NBTTagCompound getCustomData() { return customData; }
    public void setCustomData(NBTTagCompound customData) { this.customData = customData == null ? new NBTTagCompound() : customData; }
    public String getRenderState() { return RenderStateData.getRenderState(customData); }
    public void setRenderState(String renderState) {
        if (customData == null) {
            customData = new NBTTagCompound();
        }
        RenderStateData.setRenderState(customData, renderState);
    }
    public IBulletRenderer getRenderer() { return renderer; }
    public void setRenderer(IBulletRenderer renderer) { this.renderer = renderer; }
}
