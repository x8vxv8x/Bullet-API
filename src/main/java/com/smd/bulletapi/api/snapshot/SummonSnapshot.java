package com.smd.bulletapi.api.snapshot;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.common.summon.SummonState;
import com.smd.bulletapi.common.summon.SummonTargetSource;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

@PublicApi
public final class SummonSnapshot {
    public final int id;
    public final String definitionId;
    public final UUID ownerId;
    public final Vec3d position;
    public final Vec3d velocity;
    public final int life;
    public final float damage;
    public final SummonState state;
    public final int targetEntityId;
    public final SummonTargetSource targetSource;
    public final int slotCost;
    public final int formationIndex;

    public SummonSnapshot(int id, String definitionId, UUID ownerId, Vec3d position, Vec3d velocity,
                          int life, float damage, SummonState state, int targetEntityId, SummonTargetSource targetSource,
                          int slotCost, int formationIndex) {
        this.id = id;
        this.definitionId = definitionId;
        this.ownerId = ownerId;
        this.position = position;
        this.velocity = velocity;
        this.life = life;
        this.damage = damage;
        this.state = state;
        this.targetEntityId = targetEntityId;
        this.targetSource = targetSource;
        this.slotCost = slotCost;
        this.formationIndex = formationIndex;
    }

    @Override
    public String toString() {
        return "SummonSnapshot{id=" + id + ", definitionId='" + definitionId + '\''
                + ", ownerId=" + ownerId + ", position=" + position + ", velocity=" + velocity
                + ", life=" + life + ", damage=" + damage + ", state=" + state
                + ", targetEntityId=" + targetEntityId + ", targetSource=" + targetSource + ", slotCost=" + slotCost
                + ", formationIndex=" + formationIndex + "}";
    }
}
