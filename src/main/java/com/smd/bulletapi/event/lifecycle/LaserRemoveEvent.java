package com.smd.bulletapi.event.lifecycle;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.snapshot.LaserSnapshot;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

@PublicApi
public class LaserRemoveEvent extends WorldEvent {
    private final LaserSnapshot snapshot;
    private final LifecycleRemoveReason reason;

    public LaserRemoveEvent(World world, LaserSnapshot snapshot, LifecycleRemoveReason reason) {
        super(world);
        this.snapshot = snapshot;
        this.reason = reason;
    }

    public LaserSnapshot getSnapshot() {
        return snapshot;
    }

    public LifecycleRemoveReason getReason() {
        return reason;
    }
}
