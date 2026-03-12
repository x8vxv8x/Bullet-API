package com.smd.bulletapi.server;

import com.smd.bulletapi.common.LaserCollisionContext;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Laser {
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
    private final Consumer<LaserCollisionContext> onCollision;
    private final EntityLivingBase shooter;
    private final ItemStack shooterHeldItem;
    private final Map<Integer, Long> lastHitTick = new ConcurrentHashMap<>();
    public Laser(int id, Vec3d start, Vec3d direction, double maxLength, float thickness,
                 float damage, int life, boolean penetrate,
                 boolean followShooter, boolean onlyPlayer, boolean blockStops,
                 Vec3d startOffset,
                 Vec3d startOffsetLocal,
                 int eventIntervalTicks,
                 int color, String rendererType, NBTTagCompound customData,
                 Consumer<LaserCollisionContext> onCollision,
                 EntityLivingBase shooter, ItemStack shooterHeldItem) {
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
        this.onCollision = onCollision;
        this.shooter = shooter;
        this.shooterHeldItem = shooterHeldItem == null ? null : shooterHeldItem.copy();
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

    public void onCollision(LaserCollisionContext context) {
        if (onCollision != null) onCollision.accept(context);
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
    public Vec3d getDirection() { return direction; }
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
    public EntityLivingBase getShooter() { return shooter; }
    public ItemStack getShooterHeldItem() { return shooterHeldItem; }

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
