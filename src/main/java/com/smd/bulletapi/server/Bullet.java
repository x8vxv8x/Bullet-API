package com.smd.bulletapi.server;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.api.runtime.IBulletActor;
import com.smd.bulletapi.common.AttackSourceInfo;
import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.RenderStateData;
import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.common.runtime.RuntimeObject;
import com.smd.bulletapi.spi.bullet.IBulletCollisionFilter;
import com.smd.bulletapi.spi.bullet.IBulletHitBehavior;
import com.smd.bulletapi.spi.bullet.IBulletMotionController;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Consumer;

@InternalApi
public class Bullet implements IBulletActor, RuntimeObject {
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
    private IBulletHitBehavior hitBehavior;
    private IBulletMotionController motionController;
    private Consumer<IBulletActor> tickCallback; // 新增：每 tick 回调
    private IBulletCollisionFilter collisionFilter;
    private final EntityLivingBase shooter;
    private final ItemStack shooterHeldItem;
    private final AttackSourceInfo attackSourceInfo;
    private final String renderPresetId;

    public Bullet(int id, Vec3d position, Vec3d velocity, int maxLife, float damage,
                  String texture, int color, float size, String rendererType,
                  NBTTagCompound customData, ICollisionShape collisionShape,
                  IBulletHitBehavior hitBehavior,
                  IBulletMotionController motionController, Consumer<IBulletActor> tickCallback,
                  IBulletCollisionFilter collisionFilter, boolean onlyPlayer,
                  EntityLivingBase shooter, ItemStack shooterHeldItem, AttackSourceInfo attackSourceInfo,
                  String renderPresetId) {
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
        this.hitBehavior = hitBehavior;
        this.motionController = motionController;
        this.tickCallback = tickCallback;
        this.collisionFilter = collisionFilter;
        this.onlyPlayer = onlyPlayer;
        this.shooter = shooter;
        this.shooterHeldItem = shooterHeldItem == null ? null : shooterHeldItem.copy();
        this.attackSourceInfo = attackSourceInfo == null ? AttackSourceInfo.normal() : attackSourceInfo;
        this.renderPresetId = renderPresetId;
        this.attackSourceInfo.writeToTag(this.customData);
    }

    public void update(World world) {
        if (dead) return;

        if (motionController != null) {
            motionController.tick(world, this);
        }

        if (tickCallback != null) {
            tickCallback.accept(this);
        }

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
    public void setTexture(String texture) { this.texture = texture == null || texture.isEmpty() ? null : texture; }
    public int getColor() { return color; }
    public float getSize() { return size; }
    public String getRendererType() { return rendererType; }
    public void setRendererType(String rendererType) { this.rendererType = rendererType == null || rendererType.isEmpty() ? null : rendererType; }
    public NBTTagCompound getCustomData() { return customData; }
    public void setCustomData(NBTTagCompound customData) { this.customData = customData; }
    public String getRenderState() { return RenderStateData.getRenderState(customData); }
    public void setRenderState(String renderState) {
        if (customData == null) {
            customData = new NBTTagCompound();
        }
        RenderStateData.setRenderState(customData, renderState);
    }
    public ICollisionShape getCollisionShape() { return collisionShape; }
    public boolean hasCollision() { return collisionShape != null; }
    public IBulletHitBehavior getHitBehavior() { return hitBehavior; }
    public IBulletMotionController getMotionController() { return motionController; }
    public Consumer<IBulletActor> getTickCallback() { return tickCallback; }
    public void setTickCallback(Consumer<IBulletActor> tickCallback) { this.tickCallback = tickCallback; }
    public IBulletCollisionFilter getCollisionFilter() { return collisionFilter; }
    public boolean isOnlyPlayer() { return onlyPlayer; }
    public EntityLivingBase getShooter() { return shooter; }
    public ItemStack getShooterHeldItem() { return shooterHeldItem; }
    public AttackSourceInfo getAttackSourceInfo() { return attackSourceInfo; }
    public String getRenderPresetId() { return renderPresetId; }

    /**
     * 优先使用该入口，以复用同一次碰撞链路中的 CollisionContext。
     */
    public void handleHit(CollisionContext context) {
        if (hitBehavior != null) {
            hitBehavior.onHit(context);
        }
    }
}
