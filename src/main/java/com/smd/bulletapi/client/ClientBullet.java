package com.smd.bulletapi.client;

import com.smd.bulletapi.client.render.IBulletRenderer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class ClientBullet {
    private final int id;
    private Vec3d position;
    private Vec3d prevPosition;
    private Vec3d velocity;
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
        this.position = position;
        this.prevPosition = position;
        this.velocity = velocity;
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
        prevPosition = position;
        position = position.add(velocity);
        life--;
        if (life <= 0) dead = true;
    }

    public Vec3d getRenderPosition(float partialTicks) {
        double x = prevPosition.x + (position.x - prevPosition.x) * partialTicks;
        double y = prevPosition.y + (position.y - prevPosition.y) * partialTicks;
        double z = prevPosition.z + (position.z - prevPosition.z) * partialTicks;
        return new Vec3d(x, y, z);
    }

    public void getRenderPosition(float partialTicks, double[] outPos) {
        outPos[0] = prevPosition.x + (position.x - prevPosition.x) * partialTicks;
        outPos[1] = prevPosition.y + (position.y - prevPosition.y) * partialTicks;
        outPos[2] = prevPosition.z + (position.z - prevPosition.z) * partialTicks;
    }

    public int getId() { return id; }
    public boolean isDead() { return dead; }
    public void setVelocity(Vec3d velocity) { this.velocity = velocity; }
    public Vec3d getVelocity() { return velocity; }
    public ResourceLocation getTexture() { return texture; }
    public int getColor() { return color; }
    public float getSize() { return size; }
    public String getRendererType() { return rendererType; }
    public NBTTagCompound getCustomData() { return customData; }
    public IBulletRenderer getRenderer() { return renderer; }
    public void setRenderer(IBulletRenderer renderer) { this.renderer = renderer; }
}