package com.smd.bulletapi.api.runtime;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.common.summon.SummonDefinition;
import com.smd.bulletapi.common.summon.SummonState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import java.util.UUID;

@PublicApi
public interface ISummonActor extends IBulletActor {
    UUID getOwnerId();
    int getSlotCost();
    String getDefinitionId();
    SummonDefinition getDefinition();
    SummonState getState();
    void setState(SummonState state);
    int getFormationIndex();
    long getSpawnTick();
    EntityLivingBase getOwnerEntity(World world);
    EntityLivingBase getTarget(World world);
    int getTargetEntityId();
    void setTarget(EntityLivingBase target);
    boolean shouldRetarget();
    void resetRetargetCooldown();
    boolean canAttack();
    void setAttackCooldown(int attackCooldown);
    boolean shouldSync();
    void resetSyncCooldown();
    void tickCooldowns();
    boolean canTriggerContact(EntityLivingBase entity, long worldTick);
    void markContactTriggered(EntityLivingBase entity, long worldTick);
    boolean hasReleasedSlots();
    void markSlotsReleased();
    int getActiveLaserId();
    boolean hasActiveLaser();
    void setActiveLaserId(int activeLaserId);
    void clearActiveLaserId();
}
