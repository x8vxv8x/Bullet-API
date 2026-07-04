package com.smd.bulletapi.server.summon;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.api.runtime.ISummonActor;
import com.smd.bulletapi.common.AttackSourceInfo;
import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.common.data.DataPayload;
import com.smd.bulletapi.common.runtime.RuntimeObject;
import com.smd.bulletapi.common.runtime.state.ActorSourceState;
import com.smd.bulletapi.common.runtime.state.MotionState3D;
import com.smd.bulletapi.common.runtime.state.SpriteVisualState;
import com.smd.bulletapi.common.summon.SummonDefinition;
import com.smd.bulletapi.common.summon.SummonState;
import com.smd.bulletapi.common.summon.SummonTargetSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@InternalApi
public class SummonBullet implements ISummonActor, RuntimeObject {
    private final int id;
    private final MotionState3D motion;
    private final SpriteVisualState visual;
    private final ActorSourceState source;
    private final UUID ownerId;
    private final int slotCost;
    private final String definitionId;
    private final SummonDefinition definition;
    private final ICollisionShape collisionShape;
    private SummonState state = SummonState.IDLE;
    private int targetEntityId = -1;
    private SummonTargetSource targetSource = SummonTargetSource.NONE;
    private int retargetCooldown;
    private int attackCooldown;
    private int syncCooldown;
    private final int formationIndex;
    private final long spawnTick;
    private final Map<Integer, Long> lastContactHitTick = new HashMap<>();
    private int activeLaserId = -1;
    private boolean releasedSlots;
    private double lastSyncedPositionX;
    private double lastSyncedPositionY;
    private double lastSyncedPositionZ;
    private double lastSyncedVelocityX;
    private double lastSyncedVelocityY;
    private double lastSyncedVelocityZ;
    private boolean syncBaselineInitialized;
    private long lastSyncWorldTick;

    public SummonBullet(int id, Vec3d position, Vec3d velocity, SummonDefinition definition,
                        EntityLivingBase owner, int formationIndex, long spawnTick) {
        this.id = id;
        this.motion = new MotionState3D(position, velocity, definition.getLife());
        this.visual = new SpriteVisualState(
                definition.getTexture(),
                definition.getColor(),
                definition.getSize(),
                definition.getRendererType(),
                definition.getCustomData() == null ? new DataPayload() : definition.getCustomData().copy()
        );
        this.source = new ActorSourceState(false, owner, null, AttackSourceInfo.summonBody(owner.getUniqueID(), id, definition.getId()));
        this.ownerId = owner.getUniqueID();
        this.slotCost = definition.getSlotCost();
        this.definitionId = definition.getId();
        this.definition = definition;
        this.collisionShape = definition.getCollisionShape();
        this.formationIndex = formationIndex;
        this.spawnTick = spawnTick;
        this.lastSyncedPositionX = position.x;
        this.lastSyncedPositionY = position.y;
        this.lastSyncedPositionZ = position.z;
        this.lastSyncedVelocityX = velocity.x;
        this.lastSyncedVelocityY = velocity.y;
        this.lastSyncedVelocityZ = velocity.z;
        this.syncBaselineInitialized = true;
        this.lastSyncWorldTick = spawnTick;
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

    @Override
    public float getDamage() { return definition.getDamage(); }

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

    @Override
    public boolean isOnlyPlayer() { return source.isOnlyPlayer(); }

    @Override
    public EntityLivingBase getShooter() { return source.getShooter(); }

    @Override
    public ItemStack getShooterHeldItem() { return source.getShooterHeldItem(); }

    @Override
    public AttackSourceInfo getAttackSourceInfo() { return source.getAttackSourceInfo(); }

    public void update(World world) {
        if (motion.isDead()) {
            return;
        }
        motion.tickLinear();
    }

    public void handleHit(CollisionContext context) {
    }

    @Override
    public UUID getOwnerId() { return ownerId; }

    @Override
    public int getSlotCost() { return slotCost; }

    @Override
    public String getDefinitionId() { return definitionId; }

    @Override
    public SummonDefinition getDefinition() { return definition; }

    @Override
    public SummonState getState() { return state; }

    @Override
    public void setState(SummonState state) { this.state = state; }

    @Override
    public int getFormationIndex() { return formationIndex; }

    @Override
    public long getSpawnTick() { return spawnTick; }

    @Override
    public EntityLivingBase getOwnerEntity(World world) {
        EntityLivingBase shooter = getShooter();
        if (shooter != null && !shooter.isDead && shooter.world == world && ownerId.equals(shooter.getUniqueID())) {
            return shooter;
        }
        for (EntityLivingBase player : world.playerEntities) {
            if (!player.isDead && ownerId.equals(player.getUniqueID())) {
                return player;
            }
        }
        return null;
    }

    @Override
    public EntityLivingBase getTarget(World world) {
        if (targetEntityId < 0) {
            return null;
        }
        Entity entity = world.getEntityByID(targetEntityId);
        return entity instanceof EntityLivingBase && !entity.isDead ? (EntityLivingBase) entity : null;
    }

    @Override
    public int getTargetEntityId() {
        return targetEntityId;
    }

    @Override
    public void setTarget(EntityLivingBase target) {
        setTarget(target, SummonTargetSource.SCRIPT);
    }

    @Override
    public SummonTargetSource getTargetSource() {
        return targetSource;
    }

    public void setTarget(EntityLivingBase target, SummonTargetSource source) {
        this.targetEntityId = target == null ? -1 : target.getEntityId();
        this.targetSource = target == null ? SummonTargetSource.NONE : (source == null ? SummonTargetSource.SCRIPT : source);
    }

    public void clearTarget() {
        setTarget(null, SummonTargetSource.NONE);
    }

    @Override
    public boolean shouldRetarget() {
        return retargetCooldown <= 0;
    }

    @Override
    public void resetRetargetCooldown() {
        retargetCooldown = Math.max(1, definition.getRetargetIntervalTicks());
    }

    @Override
    public boolean canAttack() {
        return attackCooldown <= 0;
    }

    @Override
    public void setAttackCooldown(int attackCooldown) {
        this.attackCooldown = Math.max(0, attackCooldown);
    }

    @Override
    public boolean shouldSync() {
        return syncCooldown <= 0;
    }

    @Override
    public void resetSyncCooldown() {
        syncCooldown = Math.max(1, definition.getSyncIntervalTicks());
    }

    @Override
    public void tickCooldowns() {
        if (retargetCooldown > 0) {
            retargetCooldown--;
        }
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (syncCooldown > 0) {
            syncCooldown--;
        }
    }

    public boolean hasSyncBaseline() {
        return syncBaselineInitialized;
    }

    public double getLastSyncedPositionX() { return lastSyncedPositionX; }

    public double getLastSyncedPositionY() { return lastSyncedPositionY; }

    public double getLastSyncedPositionZ() { return lastSyncedPositionZ; }

    public double getLastSyncedVelocityX() { return lastSyncedVelocityX; }

    public double getLastSyncedVelocityY() { return lastSyncedVelocityY; }

    public double getLastSyncedVelocityZ() { return lastSyncedVelocityZ; }

    public long getLastSyncWorldTick() {
        return lastSyncWorldTick;
    }

    public void markSynced(long worldTick) {
        this.lastSyncedPositionX = motion.getPositionX();
        this.lastSyncedPositionY = motion.getPositionY();
        this.lastSyncedPositionZ = motion.getPositionZ();
        this.lastSyncedVelocityX = motion.getVelocityX();
        this.lastSyncedVelocityY = motion.getVelocityY();
        this.lastSyncedVelocityZ = motion.getVelocityZ();
        this.syncBaselineInitialized = true;
        this.lastSyncWorldTick = worldTick;
    }

    @Override
    public boolean canTriggerContact(EntityLivingBase entity, long worldTick) {
        int interval = definition.getBodyCollisionIntervalTicks();
        if (entity == null) {
            return false;
        }
        if (interval <= 0) {
            return true;
        }
        Long last = lastContactHitTick.get(entity.getEntityId());
        return last == null || worldTick - last >= interval;
    }

    @Override
    public void markContactTriggered(EntityLivingBase entity, long worldTick) {
        if (entity == null) {
            return;
        }
        lastContactHitTick.put(entity.getEntityId(), worldTick);
    }

    @Override
    public boolean hasReleasedSlots() {
        return releasedSlots;
    }

    @Override
    public void markSlotsReleased() {
        releasedSlots = true;
    }

    @Override
    public int getActiveLaserId() {
        return activeLaserId;
    }

    @Override
    public boolean hasActiveLaser() {
        return activeLaserId >= 0;
    }

    @Override
    public void setActiveLaserId(int activeLaserId) {
        this.activeLaserId = activeLaserId;
    }

    @Override
    public void clearActiveLaserId() {
        this.activeLaserId = -1;
    }
}
