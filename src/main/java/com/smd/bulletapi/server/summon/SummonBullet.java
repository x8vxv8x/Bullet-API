package com.smd.bulletapi.server.summon;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.api.runtime.ISummonActor;
import com.smd.bulletapi.common.AttackSourceInfo;
import com.smd.bulletapi.common.summon.SummonDefinition;
import com.smd.bulletapi.common.summon.SummonState;
import com.smd.bulletapi.server.Bullet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@InternalApi
public class SummonBullet extends Bullet implements ISummonActor {
    private final UUID ownerId;
    private final int slotCost;
    private final String definitionId;
    private final SummonDefinition definition;
    private SummonState state = SummonState.IDLE;
    private int targetEntityId = -1;
    private int retargetCooldown;
    private int attackCooldown;
    private int syncCooldown;
    private final int formationIndex;
    private final long spawnTick;
    private final Map<Integer, Long> lastContactHitTick = new ConcurrentHashMap<>();
    private int activeLaserId = -1;
    private boolean releasedSlots;

    public SummonBullet(int id, Vec3d position, Vec3d velocity, SummonDefinition definition,
                        EntityLivingBase owner, int formationIndex, long spawnTick) {
        super(id, position, velocity, definition.getLife(), definition.getDamage(),
                definition.getTexture(), definition.getColor(), definition.getSize(),
                definition.getRendererType(),
                definition.getCustomData() == null ? new NBTTagCompound() : definition.getCustomData().copy(),
                definition.getCollisionShape(), null, null, null, null, false, owner, null,
                AttackSourceInfo.summonBody(owner.getUniqueID(), id, definition.getId()));
        this.ownerId = owner.getUniqueID();
        this.slotCost = definition.getSlotCost();
        this.definitionId = definition.getId();
        this.definition = definition;
        this.formationIndex = formationIndex;
        this.spawnTick = spawnTick;
    }

    public UUID getOwnerId() { return ownerId; }
    public int getSlotCost() { return slotCost; }
    public String getDefinitionId() { return definitionId; }
    public SummonDefinition getDefinition() { return definition; }
    public SummonState getState() { return state; }
    public void setState(SummonState state) { this.state = state; }
    public int getFormationIndex() { return formationIndex; }
    public long getSpawnTick() { return spawnTick; }

    public EntityLivingBase getOwnerEntity(World world) {
        EntityLivingBase shooter = getShooter();
        if (shooter != null && !shooter.isDead && shooter.world == world && ownerId.equals(shooter.getUniqueID())) {
            return shooter;
        }
        for (Entity player : world.playerEntities) {
            if (player instanceof EntityLivingBase && !player.isDead && ownerId.equals(player.getUniqueID())) {
                return (EntityLivingBase) player;
            }
        }
        return null;
    }

    public EntityLivingBase getTarget(World world) {
        if (targetEntityId < 0) return null;
        Entity entity = world.getEntityByID(targetEntityId);
        return entity instanceof EntityLivingBase && !entity.isDead ? (EntityLivingBase) entity : null;
    }

    public int getTargetEntityId() {
        return targetEntityId;
    }

    public void setTarget(EntityLivingBase target) {
        this.targetEntityId = target == null ? -1 : target.getEntityId();
    }

    public boolean shouldRetarget() {
        return retargetCooldown <= 0;
    }

    public void resetRetargetCooldown() {
        retargetCooldown = Math.max(1, definition.getRetargetIntervalTicks());
    }

    public boolean canAttack() {
        return attackCooldown <= 0;
    }

    public void setAttackCooldown(int attackCooldown) {
        this.attackCooldown = Math.max(0, attackCooldown);
    }

    public boolean shouldSync() {
        return syncCooldown <= 0;
    }

    public void resetSyncCooldown() {
        syncCooldown = Math.max(1, definition.getSyncIntervalTicks());
    }

    public void tickCooldowns() {
        if (retargetCooldown > 0) retargetCooldown--;
        if (attackCooldown > 0) attackCooldown--;
        if (syncCooldown > 0) syncCooldown--;
    }

    public boolean canTriggerContact(EntityLivingBase entity, long worldTick) {
        int interval = definition.getBodyCollisionIntervalTicks();
        if (entity == null) return false;
        if (interval <= 0) return true;
        Long last = lastContactHitTick.get(entity.getEntityId());
        return last == null || worldTick - last >= interval;
    }

    public void markContactTriggered(EntityLivingBase entity, long worldTick) {
        if (entity == null) return;
        lastContactHitTick.put(entity.getEntityId(), worldTick);
    }

    public boolean hasReleasedSlots() {
        return releasedSlots;
    }

    public void markSlotsReleased() {
        releasedSlots = true;
    }

    public int getActiveLaserId() {
        return activeLaserId;
    }

    public boolean hasActiveLaser() {
        return activeLaserId >= 0;
    }

    public void setActiveLaserId(int activeLaserId) {
        this.activeLaserId = activeLaserId;
    }

    public void clearActiveLaserId() {
        this.activeLaserId = -1;
    }
}
