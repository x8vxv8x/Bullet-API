package com.smd.bulletapi.api.builder;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.handle.LaserHandle;
import com.smd.bulletapi.api.preset.LaserPreset;
import com.smd.bulletapi.api.preset.LaserPresetRegistry;
import com.smd.bulletapi.common.AttackSourceInfo;
import com.smd.bulletapi.common.DanmakuManager;
import com.smd.bulletapi.common.LaserCollisionContext;
import com.smd.bulletapi.spi.laser.ILaserCollisionFilter;
import com.smd.bulletapi.spi.laser.ILaserHitBehavior;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Consumer;

@PublicApi
public class LaserBuilder {
    private final World world;
    private Vec3d start;
    private Vec3d direction;
    private double maxLength = 20.0;
    private float thickness = 0.4f;
    private int life = -1;
    private float damage = 1.0f;
    private int color = 0xFF3333;
    private String rendererType = "laser_beam";
    private final NBTTagCompound customData = new NBTTagCompound();
    private boolean penetrate = false;
    private boolean followShooter = true;
    private boolean onlyPlayer = false;
    private boolean blockStops = true;
    private Vec3d startOffset = new Vec3d(0, 0, 0);
    private Vec3d startOffsetLocal = new Vec3d(0, 0, 0);
    private int eventIntervalTicks = 0;
    private ILaserHitBehavior hitBehavior;
    private Consumer<LaserCollisionContext> onCollision;
    private ILaserCollisionFilter collisionFilter;
    private EntityLivingBase shooter;
    private ItemStack shooterHeldItem;
    private AttackSourceInfo attackSourceInfo;

    public LaserBuilder(World world) {
        this.world = world;
    }

    public LaserBuilder start(Vec3d start) {
        this.start = start;
        return this;
    }

    public LaserBuilder direction(Vec3d direction) {
        this.direction = direction;
        return this;
    }

    public LaserBuilder startOffset(Vec3d offset) {
        this.startOffset = offset == null ? new Vec3d(0, 0, 0) : offset;
        return this;
    }

    public LaserBuilder startOffsetLocal(Vec3d offset) {
        this.startOffsetLocal = offset == null ? new Vec3d(0, 0, 0) : offset;
        return this;
    }

    public LaserBuilder eventIntervalTicks(int ticks) {
        this.eventIntervalTicks = Math.max(0, ticks);
        return this;
    }

    public LaserBuilder maxLength(double maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public LaserBuilder thickness(float thickness) {
        this.thickness = thickness;
        return this;
    }

    public LaserBuilder life(int life) {
        this.life = life;
        return this;
    }

    public LaserBuilder damage(float damage) {
        this.damage = damage;
        return this;
    }

    public LaserBuilder color(int rgb) {
        this.color = rgb;
        return this;
    }

    public LaserBuilder rendererType(String type) {
        this.rendererType = type;
        return this;
    }

    public LaserBuilder set(String key, String value) {
        customData.setString(key, value);
        return this;
    }

    public LaserBuilder set(String key, int value) {
        customData.setInteger(key, value);
        return this;
    }

    public LaserBuilder set(String key, float value) {
        customData.setFloat(key, value);
        return this;
    }

    public LaserBuilder set(String key, boolean value) {
        customData.setBoolean(key, value);
        return this;
    }

    public LaserBuilder preset(String id) {
        LaserPreset preset = LaserPresetRegistry.get(id);
        if (preset == null) {
            throw new IllegalStateException("Unknown laser preset: " + id);
        }
        preset.apply(this);
        return this;
    }

    public LaserBuilder preset(LaserPreset preset) {
        if (preset != null) {
            preset.apply(this);
        }
        return this;
    }

    public LaserBuilder penetrate(boolean penetrate) {
        this.penetrate = penetrate;
        return this;
    }

    public LaserBuilder followShooter(boolean followShooter) {
        this.followShooter = followShooter;
        return this;
    }

    public LaserBuilder onlyPlayer(boolean onlyPlayer) {
        this.onlyPlayer = onlyPlayer;
        return this;
    }

    public LaserBuilder blockStops(boolean blockStops) {
        this.blockStops = blockStops;
        return this;
    }

    public LaserBuilder hitBehavior(ILaserHitBehavior hitBehavior) {
        this.hitBehavior = hitBehavior;
        return this;
    }

    public LaserBuilder onCollision(Consumer<LaserCollisionContext> callback) {
        this.onCollision = callback;
        return this;
    }

    public LaserBuilder collisionFilter(ILaserCollisionFilter collisionFilter) {
        this.collisionFilter = collisionFilter;
        return this;
    }

    public LaserBuilder shooter(EntityLivingBase shooter) {
        this.shooter = shooter;
        return this;
    }

    public LaserBuilder shooterHeldItem(ItemStack item) {
        this.shooterHeldItem = item == null ? null : item.copy();
        return this;
    }

    public LaserBuilder attackSourceInfo(AttackSourceInfo attackSourceInfo) {
        this.attackSourceInfo = attackSourceInfo;
        return this;
    }

    public int spawn() {
        return spawnHandle().getId();
    }

    public LaserHandle spawnHandle() {
        if (world.isRemote) throw new IllegalStateException("Cannot spawn laser on client side");
        if (start == null && !followShooter) throw new IllegalStateException("Start must be set or followShooter enabled");
        if (direction == null && !followShooter) throw new IllegalStateException("Direction must be set or followShooter enabled");

        int id = DanmakuManager.getInstance().spawnLaser(
                world,
                start,
                direction,
                maxLength,
                thickness,
                life,
                damage,
                color,
                rendererType,
                customData,
                penetrate,
                followShooter,
                onlyPlayer,
                blockStops,
                startOffset,
                startOffsetLocal,
                eventIntervalTicks,
                hitBehavior,
                onCollision,
                collisionFilter,
                shooter,
                shooterHeldItem,
                attackSourceInfo
        );
        return new LaserHandle(world, id);
    }
}
