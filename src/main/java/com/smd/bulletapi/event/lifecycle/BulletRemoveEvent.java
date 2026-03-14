package com.smd.bulletapi.event.lifecycle;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.snapshot.BulletSnapshot;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

@PublicApi
public class BulletRemoveEvent extends WorldEvent {
    private final BulletSnapshot snapshot;
    private final LifecycleRemoveReason reason;

    public BulletRemoveEvent(World world, BulletSnapshot snapshot, LifecycleRemoveReason reason) {
        super(world);
        this.snapshot = snapshot;
        this.reason = reason;
    }

    public BulletSnapshot getSnapshot() {
        return snapshot;
    }

    public LifecycleRemoveReason getReason() {
        return reason;
    }
}
