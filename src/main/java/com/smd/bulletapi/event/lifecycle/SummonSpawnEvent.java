package com.smd.bulletapi.event.lifecycle;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.snapshot.SummonSnapshot;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

@PublicApi
public class SummonSpawnEvent extends WorldEvent {
    private final SummonSnapshot snapshot;

    public SummonSpawnEvent(World world, SummonSnapshot snapshot) {
        super(world);
        this.snapshot = snapshot;
    }

    public SummonSnapshot getSnapshot() {
        return snapshot;
    }
}
