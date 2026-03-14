package com.smd.bulletapi.event.lifecycle;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.handle.SummonHandle;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

@PublicApi
public class SummonTargetChangedEvent extends WorldEvent {
    private final SummonHandle handle;
    private final int previousTargetEntityId;
    private final int newTargetEntityId;

    public SummonTargetChangedEvent(World world, SummonHandle handle, int previousTargetEntityId, int newTargetEntityId) {
        super(world);
        this.handle = handle;
        this.previousTargetEntityId = previousTargetEntityId;
        this.newTargetEntityId = newTargetEntityId;
    }

    public SummonHandle getHandle() {
        return handle;
    }

    public int getPreviousTargetEntityId() {
        return previousTargetEntityId;
    }

    public int getNewTargetEntityId() {
        return newTargetEntityId;
    }
}
