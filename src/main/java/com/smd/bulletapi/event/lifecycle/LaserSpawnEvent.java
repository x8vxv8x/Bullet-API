package com.smd.bulletapi.event.lifecycle;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.snapshot.LaserSnapshot;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

@PublicApi
public class LaserSpawnEvent extends WorldEvent {
    private final LaserSnapshot snapshot;

    public LaserSpawnEvent(World world, LaserSnapshot snapshot) {
        super(world);
        this.snapshot = snapshot;
    }

    public LaserSnapshot getSnapshot() {
        return snapshot;
    }
}
