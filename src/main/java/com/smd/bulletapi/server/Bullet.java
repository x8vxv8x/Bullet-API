package com.smd.bulletapi.server;

import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.collision.ICollisionShape;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class Bullet {
    private final int id;
    private Vec3d position;
    private Vec3d velocity;
    private int life;
    private final int maxLife;
    private float damage;
    private boolean dead;
    private String texture;
    private int color;
    private float size;
    private boolean onlyPlayer;
    private String rendererType;
    private NBTTagCompound customData;
    private ICollisionShape collisionShape;
    private Consumer<CollisionContext> onCollision;
    private Consumer<Bullet> tickCallback; // 新增：每 tick 回调

    public Bullet(int id, Vec3d position, Vec3d velocity, int maxLife, float damage,
                  String texture, int color, float size, String rendererType,
                  NBTTagCompound customData, ICollisionShape collisionShape,
                  Consumer<CollisionContext> onCollision, Consumer<Bullet> tickCallback, boolean onlyPlayer) {
        this.id = id;
        this.position = position;
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
        this.collisionShape = collisionShape;
        this.onCollision = onCollision;
        this.tickCallback = tickCallback;
        this.onlyPlayer = onlyPlayer;
    }

    public void update(World world) {
        if (dead) return;

        // 调用自定义每 tick 回调（开发者可在此修改速度等）
        if (tickCallback != null) {
            tickCallback.accept(this);
        }

        // 更新位置
        position = position.add(velocity);
        life--;
        if (life <= 0) dead = true;
    }

    // Getter / Setter 方法（原有+新增）
    public int getId() { return id; }
    public Vec3d getPosition() { return position; }
    public Vec3d getVelocity() { return velocity; }
    public void setVelocity(Vec3d velocity) { this.velocity = velocity; }
    public int getLife() { return life; }
    public float getDamage() { return damage; }
    public boolean isDead() { return dead; }
    public String getTexture() { return texture; }
    public int getColor() { return color; }
    public float getSize() { return size; }
    public String getRendererType() { return rendererType; }
    public NBTTagCompound getCustomData() { return customData; }
    public void setCustomData(NBTTagCompound customData) { this.customData = customData; }
    public ICollisionShape getCollisionShape() { return collisionShape; }
    public boolean hasCollision() { return collisionShape != null; }
    public Consumer<CollisionContext> getOnCollision() { return onCollision; }
    public Consumer<Bullet> getTickCallback() { return tickCallback; }
    public void setTickCallback(Consumer<Bullet> tickCallback) { this.tickCallback = tickCallback; }
    public boolean isOnlyPlayer() { return onlyPlayer; }

    public void onCollision(World world, Entity hitEntity) {
        if (onCollision != null) {
            onCollision.accept(new CollisionContext(this, world, hitEntity));
        }
    }
}