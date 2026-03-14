package com.smd.bulletapi.server;

import com.smd.bulletapi.common.AttackSourceInfo;
import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.collision.ICollisionShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class Bullet {
    private final int id;
    private double positionX;
    private double positionY;
    private double positionZ;
    private double velocityX;
    private double velocityY;
    private double velocityZ;
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
    private final EntityLivingBase shooter;
    private final ItemStack shooterHeldItem;
    private final AttackSourceInfo attackSourceInfo;

    public Bullet(int id, Vec3d position, Vec3d velocity, int maxLife, float damage,
                  String texture, int color, float size, String rendererType,
                  NBTTagCompound customData, ICollisionShape collisionShape,
                  Consumer<CollisionContext> onCollision, Consumer<Bullet> tickCallback, boolean onlyPlayer,
                  EntityLivingBase shooter, ItemStack shooterHeldItem, AttackSourceInfo attackSourceInfo) {
        this.id = id;
        this.positionX = position.x;
        this.positionY = position.y;
        this.positionZ = position.z;
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
        this.collisionShape = collisionShape;
        this.onCollision = onCollision;
        this.tickCallback = tickCallback;
        this.onlyPlayer = onlyPlayer;
        this.shooter = shooter;
        this.shooterHeldItem = shooterHeldItem == null ? null : shooterHeldItem.copy();
        this.attackSourceInfo = attackSourceInfo == null ? AttackSourceInfo.normal() : attackSourceInfo;
        this.attackSourceInfo.writeToTag(this.customData);
    }

    public void update(World world) {
        if (dead) return;

        // 调用自定义每 tick 回调（开发者可在此修改速度等）
        if (tickCallback != null) {
            tickCallback.accept(this);
        }

        // 更新位置
        positionX += velocityX;
        positionY += velocityY;
        positionZ += velocityZ;
        life--;
        if (life <= 0) dead = true;
    }

    // Getter / Setter 方法
    public int getId() { return id; }
    public Vec3d getPosition() { return new Vec3d(positionX, positionY, positionZ); }
    public double getPosX() { return positionX; }
    public double getPosY() { return positionY; }
    public double getPosZ() { return positionZ; }
    public Vec3d getVelocity() { return new Vec3d(velocityX, velocityY, velocityZ); }
    public void setVelocity(Vec3d velocity) {
        this.velocityX = velocity.x;
        this.velocityY = velocity.y;
        this.velocityZ = velocity.z;
    }
    public void setVelocity(double x, double y, double z) {
        this.velocityX = x;
        this.velocityY = y;
        this.velocityZ = z;
    }
    public void setPosition(Vec3d position) {
        setPosition(position.x, position.y, position.z);
    }
    public void setPosition(double x, double y, double z) {
        this.positionX = x;
        this.positionY = y;
        this.positionZ = z;
    }
    public void setLife(int life) { this.life = life; }
    public void markDead() { this.dead = true; }
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
    public EntityLivingBase getShooter() { return shooter; }
    public ItemStack getShooterHeldItem() { return shooterHeldItem; }
    public AttackSourceInfo getAttackSourceInfo() { return attackSourceInfo; }

    /**
     * 优先使用该入口，以复用同一次碰撞链路中的 CollisionContext。
     */
    public void onCollision(CollisionContext context) {
        if (onCollision != null) {
            onCollision.accept(context);
        }
    }
}
