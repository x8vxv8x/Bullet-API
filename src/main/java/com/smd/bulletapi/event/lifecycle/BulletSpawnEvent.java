package com.smd.bulletapi.event.lifecycle;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.snapshot.BulletSnapshot;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

@PublicApi
public class BulletSpawnEvent extends WorldEvent {
    private final BulletSnapshot snapshot;

    public BulletSpawnEvent(World world, BulletSnapshot snapshot) {
        super(world);
        this.snapshot = snapshot;
    }

    public BulletSnapshot getSnapshot() {
        return snapshot;
    }
}
