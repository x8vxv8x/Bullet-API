package com.smd.bulletapi.common.runtime.summon;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.api.snapshot.SummonSnapshot;
import com.smd.bulletapi.common.runtime.RuntimeSnapshotFactory;
import com.smd.bulletapi.server.summon.SummonBullet;

@InternalApi
public final class SummonSnapshotFactory implements RuntimeSnapshotFactory<SummonBullet, SummonSnapshot> {
    @Override
    public SummonSnapshot create(SummonBullet summon) {
        return new SummonSnapshot(
                summon.getId(),
                summon.getDefinitionId(),
                summon.getOwnerId(),
                summon.getPosition(),
                summon.getVelocity(),
                summon.getLife(),
                summon.getDamage(),
                summon.getState(),
                summon.getTargetEntityId(),
                summon.getTargetSource(),
                summon.getSlotCost(),
                summon.getFormationIndex()
        );
    }
}
