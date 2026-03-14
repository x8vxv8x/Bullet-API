package com.smd.bulletapi.server;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.api.runtime.ILaserActor;
import com.smd.bulletapi.common.AttackSourceInfo;
import com.smd.bulletapi.common.LaserCollisionContext;
import com.smd.bulletapi.spi.laser.ILaserCollisionFilter;
import com.smd.bulletapi.spi.laser.ILaserHitBehavior;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@InternalApi
public class Laser implements ILaserActor {
    private final int id;
    private Vec3d start;
    private Vec3d direction;
    private final double maxLength;
    private double currentLength;
    private final float thickness;
    private final float damage;
    private boolean dead;
    private int life;
    private final boolean penetrate;
    private final boolean followShooter;
    private final boolean onlyPlayer;
    private final boolean blockStops;
    private final Vec3d startOffset;
    private final Vec3d startOffsetLocal;
    private final int eventIntervalTicks;
    private final int color;
    private final String rendererType;
    private NBTTagCompound customData;
    private final ILaserHitBehavior hitBehavior;
    private final ILaserCollisionFilter collisionFilter;
    private final EntityLivingBase shooter;
    private final ItemStack shooterHeldItem;
    private final AttackSourceInfo attackSourceInfo;
    private final Map<Integer, Long> lastHitTick = new ConcurrentHashMap<>();
    public Laser(int id, Vec3d start, Vec3d direction, double maxLength, float thickness,
                 float damage, int life, boolean penetrate,
                 boolean followShooter, boolean onlyPlayer, boolean blockStops,
                 Vec3d startOffset,
                 Vec3d startOffsetLocal,
                 int eventIntervalTicks,
                 ILaserHitBehavior hitBehavior,
                 int color, String rendererType, NBTTagCompound customData,
                 ILaserCollisionFilter collisionFilter,
                 EntityLivingBase shooter, ItemStack shooterHeldItem,
                 AttackSourceInfo attackSourceInfo) {
        this.id = id;
        this.start = start;
        this.direction = direction == null ? new Vec3d(0, 0, 0) : direction.normalize();
        this.maxLength = maxLength;
        this.currentLength = maxLength;
        this.thickness = thickness;
        this.damage = damage;
        this.life = life;
        this.penetrate = penetrate;
        this.followShooter = followShooter;
        this.onlyPlayer = onlyPlayer;
        this.blockStops = blockStops;
        this.startOffset = startOffset == null ? new Vec3d(0, 0, 0) : startOffset;
        this.startOffsetLocal = startOffsetLocal == null ? new Vec3d(0, 0, 0) : startOffsetLocal;
        this.eventIntervalTicks = Math.max(0, eventIntervalTicks);
        this.color = color;
        this.rendererType = rendererType;
        this.customData = customData == null ? new NBTTagCompound() : customData;
        this.hitBehavior = hitBehavior;
        this.collisionFilter = collisionFilter;
        this.shooter = shooter;
        this.shooterHeldItem = shooterHeldItem == null ? null : shooterHeldItem.copy();
        this.attackSourceInfo = attackSourceInfo == null ? AttackSourceInfo.fromTag(this.customData) : attackSourceInfo;
        this.attackSourceInfo.writeToTag(this.customData);
    }

    public void update(World world) {
        if (dead) return;
        if (shooter != null && shooter.isDead) {
            dead = true;
            return;
        }
        if (followShooter && shooter != null) {
            Vec3d eye = shooter.getPositionEyes(1.0f);
            Vec3d look = shooter.getLookVec();
            if (look.lengthSquared() > 1.0E-6) {
                Vec3d forward = look.normalize();
                Vec3d localOffset = toLocalOffset(forward, startOffsetLocal);
                start = eye.add(startOffset).add(localOffset);
                direction = forward;
            }
        }
        if (life > 0) {
            life--;
            if (life <= 0) dead = true;
        }
    }

    public void handleHit(LaserCollisionContext context) {
        if (hitBehavior != null) {
            hitBehavior.onHit(context);
        }
    }

    public boolean canTrigger(EntityLivingBase entity, long worldTick) {
        if (eventIntervalTicks <= 0) return true;
        Long last = lastHitTick.get(entity.getEntityId());
        return last == null || worldTick - last >= eventIntervalTicks;
    }

    public void markTriggered(EntityLivingBase entity, long worldTick) {
        if (eventIntervalTicks > 0) {
            lastHitTick.put(entity.getEntityId(), worldTick);
        }
    }

    public int getId() { return id; }
    public Vec3d getStart() { return start; }
    public void setStart(Vec3d start) {
        this.start = start == null ? new Vec3d(0, 0, 0) : start;
    }
    public Vec3d getDirection() { return direction; }
    public void setDirection(Vec3d direction) {
        if (direction == null || direction.lengthSquared() < 1.0E-6) {
            this.direction = new Vec3d(0, 0, 1);
            return;
        }
        this.direction = direction.normalize();
    }
    public double getMaxLength() { return maxLength; }
    public double getCurrentLength() { return currentLength; }
    public void setCurrentLength(double length) { this.currentLength = length; }
    public float getThickness() { return thickness; }
    public float getDamage() { return damage; }
    public boolean isDead() { return dead; }
    public boolean isPenetrate() { return penetrate; }
    public boolean isFollowShooter() { return followShooter; }
    public boolean isOnlyPlayer() { return onlyPlayer; }
    public boolean isBlockStops() { return blockStops; }
    public Vec3d getStartOffset() { return startOffset; }
    public Vec3d getStartOffsetLocal() { return startOffsetLocal; }
    public int getEventIntervalTicks() { return eventIntervalTicks; }
    public int getColor() { return color; }
    public String getRendererType() { return rendererType; }
    public NBTTagCompound getCustomData() { return customData; }
    public ILaserHitBehavior getHitBehavior() { return hitBehavior; }
    public ILaserCollisionFilter getCollisionFilter() { return collisionFilter; }
    public EntityLivingBase getShooter() { return shooter; }
    public ItemStack getShooterHeldItem() { return shooterHeldItem; }
    public AttackSourceInfo getAttackSourceInfo() { return attackSourceInfo; }
    public void markDead() { this.dead = true; }
    public int getLife() { return life; }

    private static Vec3d toLocalOffset(Vec3d forward, Vec3d local) {
        if (local == null) return new Vec3d(0, 0, 0);
        Vec3d upRef = new Vec3d(0, 1, 0);
        Vec3d right = forward.crossProduct(upRef);
        if (right.lengthSquared() < 1.0E-6) {
            upRef = new Vec3d(1, 0, 0);
            right = forward.crossProduct(upRef);
        }
        right = right.normalize();
        Vec3d up = right.crossProduct(forward).normalize();
        return right.scale(local.x).add(up.scale(local.y)).add(forward.scale(local.z));
    }
}
