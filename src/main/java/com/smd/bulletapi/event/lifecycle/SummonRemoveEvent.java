package com.smd.bulletapi.event.lifecycle;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.snapshot.SummonSnapshot;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

@PublicApi
public class SummonRemoveEvent extends WorldEvent {
    private final SummonSnapshot snapshot;
    private final LifecycleRemoveReason reason;

    public SummonRemoveEvent(World world, SummonSnapshot snapshot, LifecycleRemoveReason reason) {
        super(world);
        this.snapshot = snapshot;
        this.reason = reason;
    }

    public SummonSnapshot getSnapshot() {
        return snapshot;
    }

    public LifecycleRemoveReason getReason() {
        return reason;
    }
}
