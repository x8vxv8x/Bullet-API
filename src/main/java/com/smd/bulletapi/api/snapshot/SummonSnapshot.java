package com.smd.bulletapi.api.snapshot;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.common.summon.SummonState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

@PublicApi
public final class SummonSnapshot {
    private final int id;
    private final String definitionId;
    private final UUID ownerId;
    private final Vec3d position;
    private final Vec3d velocity;
    private final int life;
    private final float damage;
    private final int color;
    private final float size;
    private final String rendererType;
    private final NBTTagCompound customData;
    private final SummonState state;
    private final int targetEntityId;
    private final int slotCost;
    private final int formationIndex;

    public SummonSnapshot(int id, String definitionId, UUID ownerId, Vec3d position, Vec3d velocity,
                          int life, float damage, int color, float size, String rendererType,
                          NBTTagCompound customData, SummonState state, int targetEntityId,
                          int slotCost, int formationIndex) {
        this.id = id;
        this.definitionId = definitionId;
        this.ownerId = ownerId;
        this.position = position;
        this.velocity = velocity;
        this.life = life;
        this.damage = damage;
        this.color = color;
        this.size = size;
        this.rendererType = rendererType;
        this.customData = customData == null ? new NBTTagCompound() : customData.copy();
        this.state = state;
        this.targetEntityId = targetEntityId;
        this.slotCost = slotCost;
        this.formationIndex = formationIndex;
    }

    public int getId() { return id; }
    public String getDefinitionId() { return definitionId; }
    public UUID getOwnerId() { return ownerId; }
    public Vec3d getPosition() { return position; }
    public Vec3d getVelocity() { return velocity; }
    public int getLife() { return life; }
    public float getDamage() { return damage; }
    public int getColor() { return color; }
    public float getSize() { return size; }
    public String getRendererType() { return rendererType; }
    public NBTTagCompound getCustomData() { return customData.copy(); }
    public SummonState getState() { return state; }
    public int getTargetEntityId() { return targetEntityId; }
    public int getSlotCost() { return slotCost; }
    public int getFormationIndex() { return formationIndex; }
}
