package com.smd.bulletapi.api.builder;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.handle.BulletHandle;
import com.smd.bulletapi.api.preset.BulletPreset;
import com.smd.bulletapi.api.preset.BulletPresetRegistry;
import com.smd.bulletapi.api.runtime.IBulletActor;
import com.smd.bulletapi.common.AttackSourceInfo;
import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.DanmakuManager;
import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.spi.bullet.IBulletCollisionFilter;
import com.smd.bulletapi.spi.bullet.IBulletHitBehavior;
import com.smd.bulletapi.spi.bullet.IBulletMotionController;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Consumer;

@PublicApi
public class BulletBuilder {
    private final World world;
    private Vec3d position;
    private Vec3d velocity;
    private int life = 100;
    private float damage = 1.0f;
    private String texture;
    private int color = 0xFFFFFF;
    private float size = 0.5f;
    private String rendererType;
    private final NBTTagCompound customData = new NBTTagCompound();
    private ICollisionShape collisionShape;
    private IBulletHitBehavior hitBehavior;
    private Consumer<CollisionContext> onCollision;
    private IBulletMotionController motionController;
    private Consumer<IBulletActor> tickCallback;
    private IBulletCollisionFilter collisionFilter;
    private boolean onlyPlayer = false;
    private EntityLivingBase shooter;
    private ItemStack shooterHeldItem;
    private AttackSourceInfo attackSourceInfo;

    public BulletBuilder(World world) {
        this.world = world;
    }

    public BulletBuilder position(Vec3d pos) {
        this.position = pos;
        return this;
    }

    public BulletBuilder velocity(Vec3d vel) {
        this.velocity = vel;
        return this;
    }

    public BulletBuilder life(int life) {
        this.life = life;
        return this;
    }

    public BulletBuilder damage(float damage) {
        this.damage = damage;
        return this;
    }

    public BulletBuilder texture(String texture) {
        this.texture = texture;
        return this;
    }

    public BulletBuilder onlyPlayer(boolean onlyPlayer) {
        this.onlyPlayer = onlyPlayer;
        return this;
    }

    public BulletBuilder color(int rgb) {
        this.color = rgb;
        return this;
    }

    public BulletBuilder size(float size) {
        this.size = size;
        return this;
    }

    public BulletBuilder rendererType(String type) {
        this.rendererType = type;
        return this;
    }

    public BulletBuilder set(String key, String value) {
        customData.setString(key, value);
        return this;
    }

    public BulletBuilder set(String key, int value) {
        customData.setInteger(key, value);
        return this;
    }

    public BulletBuilder set(String key, float value) {
        customData.setFloat(key, value);
        return this;
    }

    public BulletBuilder set(String key, boolean value) {
        customData.setBoolean(key, value);
        return this;
    }

    public BulletBuilder preset(String id) {
        BulletPreset preset = BulletPresetRegistry.get(id);
        if (preset == null) {
            throw new IllegalStateException("Unknown bullet preset: " + id);
        }
        preset.apply(this);
        return this;
    }

    public BulletBuilder preset(BulletPreset preset) {
        if (preset != null) {
            preset.apply(this);
        }
        return this;
    }

    public BulletBuilder collisionShape(ICollisionShape shape) {
        this.collisionShape = shape;
        return this;
    }

    public BulletBuilder hitBehavior(IBulletHitBehavior hitBehavior) {
        this.hitBehavior = hitBehavior;
        return this;
    }

    public BulletBuilder onCollision(Consumer<CollisionContext> callback) {
        this.onCollision = callback;
        return this;
    }

    public BulletBuilder motionController(IBulletMotionController motionController) {
        this.motionController = motionController;
        return this;
    }

    public BulletBuilder onTick(Consumer<IBulletActor> callback) {
        this.tickCallback = callback;
        return this;
    }

    public BulletBuilder collisionFilter(IBulletCollisionFilter collisionFilter) {
        this.collisionFilter = collisionFilter;
        return this;
    }

    public BulletBuilder shooter(EntityLivingBase shooter) {
        this.shooter = shooter;
        return this;
    }

    public BulletBuilder shooterHeldItem(ItemStack item) {
        this.shooterHeldItem = item == null ? null : item.copy();
        return this;
    }

    public BulletBuilder attackSourceInfo(AttackSourceInfo attackSourceInfo) {
        this.attackSourceInfo = attackSourceInfo;
        return this;
    }

    public int spawn() {
        return spawnHandle().getId();
    }

    public BulletHandle spawnHandle() {
        if (position == null) throw new IllegalStateException("Position must be set");
        if (velocity == null) throw new IllegalStateException("Velocity must be set");
        if (world.isRemote) throw new IllegalStateException("Cannot spawn bullet on client side");

        int id = DanmakuManager.getInstance().spawnBullet(
                world, position, velocity, life, damage,
                texture, color, size, rendererType, customData,
                collisionShape, hitBehavior, onCollision, motionController, tickCallback, collisionFilter, onlyPlayer,
                shooter, shooterHeldItem, attackSourceInfo
        );
        return new BulletHandle(world, id);
    }
}
