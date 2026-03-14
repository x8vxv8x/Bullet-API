package com.smd.bulletapi.event.lifecycle;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.handle.SummonHandle;
import com.smd.bulletapi.common.summon.SummonState;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

@PublicApi
public class SummonStateChangedEvent extends WorldEvent {
    private final SummonHandle handle;
    private final SummonState previousState;
    private final SummonState newState;

    public SummonStateChangedEvent(World world, SummonHandle handle, SummonState previousState, SummonState newState) {
        super(world);
        this.handle = handle;
        this.previousState = previousState;
        this.newState = newState;
    }

    public SummonHandle getHandle() {
        return handle;
    }

    public SummonState getPreviousState() {
        return previousState;
    }

    public SummonState getNewState() {
        return newState;
    }
}
