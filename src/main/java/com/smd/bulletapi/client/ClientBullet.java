package com.smd.bulletapi.client;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.client.render.IBulletRenderer;
import com.smd.bulletapi.common.RenderStateData;
import com.smd.bulletapi.common.data.DataPayload;
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
    protected DataPayload customData;          // 其他自定义数据
    protected IBulletRenderer renderer;           // 渲染器实例

    public ClientBullet(int id, Vec3d position, Vec3d velocity, int maxLife, float damage,
                        ResourceLocation texture, int color, float size, String rendererType,
                        DataPayload customData) {
        this(id, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z,
                maxLife, damage, texture, color, size, rendererType, customData);
    }

    public ClientBullet(int id, double positionX, double positionY, double positionZ,
                        double velocityX, double velocityY, double velocityZ,
                        int maxLife, float damage,
                        ResourceLocation texture, int color, float size, String rendererType,
                        DataPayload customData) {
        this.id = id;
        this.positionX = positionX;
        this.positionY = positionY;
        this.positionZ = positionZ;
        this.prevPositionX = positionX;
        this.prevPositionY = positionY;
        this.prevPositionZ = positionZ;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.maxLife = maxLife;
        this.life = maxLife;
        this.damage = damage;
        this.dead = false;
        this.texture = texture;
        this.color = color;
        this.size = size;
        this.rendererType = rendererType;
        this.customData = customData == null ? new DataPayload() : customData;
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
        applySnapshot(position.x, position.y, position.z, velocity.x, velocity.y, velocity.z, life);
    }
    public void applySnapshot(double positionX, double positionY, double positionZ,
                              double velocityX, double velocityY, double velocityZ, int life) {
        this.prevPositionX = this.positionX;
        this.prevPositionY = this.positionY;
        this.prevPositionZ = this.positionZ;
        this.positionX = positionX;
        this.positionY = positionY;
        this.positionZ = positionZ;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.life = life;
        this.dead = life <= 0;
    }
    public void applyUpdate(Vec3d position, Vec3d velocity, Integer life) {
        applyUpdate(position != null, position != null ? position.x : 0.0D, position != null ? position.y : 0.0D,
                position != null ? position.z : 0.0D, velocity != null, velocity != null ? velocity.x : 0.0D,
                velocity != null ? velocity.y : 0.0D, velocity != null ? velocity.z : 0.0D, life);
    }
    public void applyUpdate(boolean hasPosition, double positionX, double positionY, double positionZ,
                            boolean hasVelocity, double velocityX, double velocityY, double velocityZ,
                            Integer life) {
        if (hasPosition) {
            this.prevPositionX = this.positionX;
            this.prevPositionY = this.positionY;
            this.prevPositionZ = this.positionZ;
            this.positionX = positionX;
            this.positionY = positionY;
            this.positionZ = positionZ;
        }
        if (hasVelocity) {
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.velocityZ = velocityZ;
        }
        if (life != null) {
            this.life = life;
            this.dead = life <= 0;
        }
    }
    public void setVelocity(Vec3d velocity) {
        setVelocity(velocity.x, velocity.y, velocity.z);
    }
    public void setVelocity(double velocityX, double velocityY, double velocityZ) {
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
    }
    public Vec3d getVelocity() { return new Vec3d(velocityX, velocityY, velocityZ); }
    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public double getVelocityZ() { return velocityZ; }
    public ResourceLocation getTexture() { return texture; }
    public void setTexture(ResourceLocation texture) { this.texture = texture; }
    public int getColor() { return color; }
    public float getSize() { return size; }
    public String getRendererType() { return rendererType; }
    public void setRendererType(String rendererType) { this.rendererType = rendererType == null || rendererType.isEmpty() ? null : rendererType; }
    public DataPayload getCustomData() { return customData; }
    public void setCustomData(DataPayload customData) { this.customData = customData == null ? new DataPayload() : customData; }
    public String getRenderState() { return RenderStateData.getRenderState(customData); }
    public void setRenderState(String renderState) {
        if (customData == null) {
            customData = new DataPayload();
        }
        RenderStateData.setRenderState(customData, renderState);
    }
    public IBulletRenderer getRenderer() { return renderer; }
    public void setRenderer(IBulletRenderer renderer) { this.renderer = renderer; }
}
