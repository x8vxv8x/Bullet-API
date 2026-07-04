package com.smd.bulletapi.server;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.api.runtime.IBulletActor;
import com.smd.bulletapi.common.AttackSourceInfo;
import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.common.data.DataPayload;
import com.smd.bulletapi.common.runtime.RuntimeObject;
import com.smd.bulletapi.common.runtime.state.ActorSourceState;
import com.smd.bulletapi.common.runtime.state.MotionState3D;
import com.smd.bulletapi.common.runtime.state.SpriteVisualState;
import com.smd.bulletapi.spi.bullet.IBulletCollisionFilter;
import com.smd.bulletapi.spi.bullet.IBulletHitBehavior;
import com.smd.bulletapi.spi.bullet.IBulletMotionController;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Consumer;

@InternalApi
public class Bullet implements IBulletActor, RuntimeObject {
    private final int id;
    private final MotionState3D motion;
    private final SpriteVisualState visual;
    private final ActorSourceState source;
    private final int maxLife;
    private float damage;
    private ICollisionShape collisionShape;
    private IBulletHitBehavior hitBehavior;
    private IBulletMotionController motionController;
    private Consumer<IBulletActor> tickCallback;
    private IBulletCollisionFilter collisionFilter;
    private final String renderPresetId;

    public Bullet(int id, Vec3d position, Vec3d velocity, int maxLife, float damage,
                  String texture, int color, float size, String rendererType,
                  DataPayload customData, ICollisionShape collisionShape,
                  IBulletHitBehavior hitBehavior,
                  IBulletMotionController motionController, Consumer<IBulletActor> tickCallback,
                  IBulletCollisionFilter collisionFilter, boolean onlyPlayer,
                  EntityLivingBase shooter, ItemStack shooterHeldItem, AttackSourceInfo attackSourceInfo,
                  String renderPresetId) {
        this.id = id;
        this.motion = new MotionState3D(position, velocity, maxLife);
        this.visual = new SpriteVisualState(texture, color, size, rendererType, customData);
        this.source = new ActorSourceState(onlyPlayer, shooter, shooterHeldItem, attackSourceInfo);
        this.maxLife = maxLife;
        this.damage = damage;
        this.collisionShape = collisionShape;
        this.hitBehavior = hitBehavior;
        this.motionController = motionController;
        this.tickCallback = tickCallback;
        this.collisionFilter = collisionFilter;
        this.renderPresetId = renderPresetId;
    }

    public void update(World world) {
        if (motion.isDead()) {
            return;
        }

        if (motionController != null) {
            motionController.tick(world, this);
        }

        if (tickCallback != null) {
            tickCallback.accept(this);
        }

        motion.tickLinear();
    }

    @Override
    public int getId() { return id; }

    @Override
    public Vec3d getPosition() { return motion.getPosition(); }

    public double getPosX() { return motion.getPositionX(); }

    public double getPosY() { return motion.getPositionY(); }

    public double getPosZ() { return motion.getPositionZ(); }

    @Override
    public Vec3d getVelocity() { return motion.getVelocity(); }

    public double getVelX() { return motion.getVelocityX(); }

    public double getVelY() { return motion.getVelocityY(); }

    public double getVelZ() { return motion.getVelocityZ(); }

    @Override
    public void setVelocity(Vec3d velocity) {
        motion.setVelocity(velocity);
    }

    @Override
    public void setVelocity(double x, double y, double z) {
        motion.setVelocity(x, y, z);
    }

    @Override
    public void setPosition(Vec3d position) {
        motion.setPosition(position);
    }

    @Override
    public void setPosition(double x, double y, double z) {
        motion.setPosition(x, y, z);
    }

    @Override
    public void setLife(int life) {
        motion.setLife(life);
    }

    @Override
    public void markDead() {
        motion.markDead();
    }

    @Override
    public int getLife() { return motion.getLife(); }

    public int getMaxLife() { return maxLife; }

    @Override
    public float getDamage() { return damage; }

    @Override
    public boolean isDead() { return motion.isDead(); }

    @Override
    public String getTexture() { return visual.getTexture(); }

    public void setTexture(String texture) { visual.setTexture(texture); }

    @Override
    public int getColor() { return visual.getColor(); }

    @Override
    public float getSize() { return visual.getSize(); }

    @Override
    public String getRendererType() { return visual.getRendererType(); }

    public void setRendererType(String rendererType) { visual.setRendererType(rendererType); }

    @Override
    public DataPayload getCustomData() { return visual.getCustomData(); }

    @Override
    public void setCustomData(DataPayload customData) { visual.setCustomData(customData); }

    public String getRenderState() { return visual.getRenderState(); }

    public void setRenderState(String renderState) { visual.setRenderState(renderState); }

    public ICollisionShape getCollisionShape() { return collisionShape; }

    public boolean hasCollision() { return collisionShape != null; }

    public IBulletHitBehavior getHitBehavior() { return hitBehavior; }

    public IBulletMotionController getMotionController() { return motionController; }

    public Consumer<IBulletActor> getTickCallback() { return tickCallback; }

    public void setTickCallback(Consumer<IBulletActor> tickCallback) { this.tickCallback = tickCallback; }

    public IBulletCollisionFilter getCollisionFilter() { return collisionFilter; }

    @Override
    public boolean isOnlyPlayer() { return source.isOnlyPlayer(); }

    @Override
    public EntityLivingBase getShooter() { return source.getShooter(); }

    @Override
    public ItemStack getShooterHeldItem() { return source.getShooterHeldItem(); }

    @Override
    public AttackSourceInfo getAttackSourceInfo() { return source.getAttackSourceInfo(); }

    public String getRenderPresetId() { return renderPresetId; }

    public void handleHit(CollisionContext context) {
        if (hitBehavior != null) {
            hitBehavior.onHit(context);
        }
    }
}
