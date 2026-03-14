package com.smd.bulletapi.api.handle;

import com.smd.bulletapi.api.SummonApi;
import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.snapshot.SummonSnapshot;
import com.smd.bulletapi.api.summon.SummonCommand;
import net.minecraft.world.World;

@PublicApi
public final class SummonHandle {
    private final World world;
    private final int id;

    public SummonHandle(World world, int id) {
        this.world = world;
        this.id = id;
    }

    public World getWorld() { return world; }
    public int getId() { return id; }
    public boolean exists() { return SummonApi.exists(world, id); }
    public boolean supportsCommand(String commandId) { return SummonApi.supportsCommand(world, id, commandId); }
    public boolean sendCommand(SummonCommand command) { return SummonApi.sendCommand(world, id, command); }
    public void remove() { SummonApi.remove(world, id); }
    public SummonSnapshot snapshot() { return SummonApi.snapshot(world, id); }
}
