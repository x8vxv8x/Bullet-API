package com.smd.bulletapi.api.handle;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.snapshot.SummonSnapshot;
import com.smd.bulletapi.api.summon.SummonCommand;
import com.smd.bulletapi.common.summon.SummonManager;
import com.smd.bulletapi.common.summon.SummonState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
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
    public boolean exists() { return SummonManager.getInstance().hasSummon(world, id); }
    public boolean supportsCommand(String commandId) { return SummonManager.getInstance().supportsCommand(world, id, commandId); }
    public boolean sendCommand(SummonCommand command) { return SummonManager.getInstance().sendCommand(world, id, command); }
    public void remove() { SummonManager.getInstance().removeSummon(world, id); }
    public void setPosition(Vec3d position) { SummonManager.getInstance().updateSummonPosition(world, id, position); }
    public void setVelocity(Vec3d velocity) { SummonManager.getInstance().updateSummonVelocity(world, id, velocity); }
    public void setMotion(Vec3d position, Vec3d velocity) { SummonManager.getInstance().updateSummonMotion(world, id, position, velocity); }
    public void setLife(int life) { SummonManager.getInstance().updateSummonLife(world, id, life); }
    public void setTarget(EntityLivingBase target) { SummonManager.getInstance().updateSummonTarget(world, id, target); }
    public void clearTarget() { SummonManager.getInstance().updateSummonTarget(world, id, null); }
    public void setState(SummonState state) { SummonManager.getInstance().updateSummonState(world, id, state); }
    public SummonSnapshot snapshot() { return SummonManager.getInstance().getSummonSnapshot(world, id); }
}
