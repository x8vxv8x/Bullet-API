package com.smd.bulletapi.server.summon;

import com.smd.bulletapi.common.summon.SummonDefinition;
import com.smd.bulletapi.common.summon.SummonState;
import com.smd.bulletapi.server.Bullet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

public class SummonBullet extends Bullet {
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
    private boolean releasedSlots;

    public SummonBullet(int id, Vec3d position, Vec3d velocity, SummonDefinition definition,
                        EntityLivingBase owner, int formationIndex) {
        super(id, position, velocity, definition.getLife(), definition.getDamage(),
                definition.getTexture(), definition.getColor(), definition.getSize(),
                definition.getRendererType(),
                definition.getCustomData() == null ? new NBTTagCompound() : definition.getCustomData().copy(),
                definition.getCollisionShape(), null, null, false, owner, null);
        this.ownerId = owner.getUniqueID();
        this.slotCost = definition.getSlotCost();
        this.definitionId = definition.getId();
        this.definition = definition;
        this.formationIndex = formationIndex;
    }

    public UUID getOwnerId() { return ownerId; }
    public int getSlotCost() { return slotCost; }
    public String getDefinitionId() { return definitionId; }
    public SummonDefinition getDefinition() { return definition; }
    public SummonState getState() { return state; }
    public void setState(SummonState state) { this.state = state; }
    public int getFormationIndex() { return formationIndex; }

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

    public boolean hasReleasedSlots() {
        return releasedSlots;
    }

    public void markSlotsReleased() {
        releasedSlots = true;
    }
}
