package com.smd.bulletapi.api.handle;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.snapshot.SummonSnapshot;
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
    public SummonHandle remove() { SummonManager.getInstance().removeSummon(world, id); return this; }
    public SummonHandle despawn() { return remove(); }
    public SummonHandle setPosition(Vec3d position) { SummonManager.getInstance().updateSummonPosition(world, id, position); return this; }
    public SummonHandle setVelocity(Vec3d velocity) { SummonManager.getInstance().updateSummonVelocity(world, id, velocity); return this; }
    public SummonHandle setMotion(Vec3d position, Vec3d velocity) { SummonManager.getInstance().updateSummonMotion(world, id, position, velocity); return this; }
    public SummonHandle setLife(int life) { SummonManager.getInstance().updateSummonLife(world, id, life); return this; }
    public SummonHandle setTexture(String texture) { SummonManager.getInstance().updateSummonTexture(world, id, texture); return this; }
    public SummonHandle setRendererType(String rendererType) { SummonManager.getInstance().updateSummonRendererType(world, id, rendererType); return this; }
    public SummonHandle setRenderState(String renderState) { SummonManager.getInstance().updateSummonRenderState(world, id, renderState); return this; }
    public SummonHandle setVisual(String texture, String rendererType, String renderState) {
        int flags = com.smd.bulletapi.network.SPacketBulletVisual.FLAG_TEXTURE
                | com.smd.bulletapi.network.SPacketBulletVisual.FLAG_RENDERER
                | com.smd.bulletapi.network.SPacketBulletVisual.FLAG_RENDER_STATE;
        SummonManager.getInstance().updateSummonVisual(world, id, texture, rendererType, renderState, flags);
        return this;
    }
    public SummonHandle clearRenderState() { SummonManager.getInstance().updateSummonRenderState(world, id, null); return this; }
    public SummonHandle setTarget(EntityLivingBase target) { SummonManager.getInstance().updateSummonTarget(world, id, target); return this; }
    public SummonHandle clearTarget() { SummonManager.getInstance().updateSummonTarget(world, id, null); return this; }
    public SummonHandle retarget() { SummonManager.getInstance().retargetSummon(world, id); return this; }
    public SummonHandle returnToOwner() { SummonManager.getInstance().returnSummonToOwner(world, id); return this; }
    public SummonHandle setState(SummonState state) { SummonManager.getInstance().updateSummonState(world, id, state); return this; }
    public SummonHandle setMode(String mode) { SummonManager.getInstance().updateSummonMode(world, id, mode); return this; }
    public SummonHandle clearMode() { SummonManager.getInstance().clearSummonMode(world, id); return this; }
    public SummonHandle setInt(String key, int value) { SummonManager.getInstance().updateSummonIntParam(world, id, key, value); return this; }
    public SummonHandle setFloat(String key, float value) { SummonManager.getInstance().updateSummonFloatParam(world, id, key, value); return this; }
    public SummonHandle setBool(String key, boolean value) { SummonManager.getInstance().updateSummonBoolParam(world, id, key, value); return this; }
    public SummonHandle setString(String key, String value) { SummonManager.getInstance().updateSummonStringParam(world, id, key, value); return this; }
    public SummonHandle clearParam(String key) { SummonManager.getInstance().clearSummonParam(world, id, key); return this; }
    public SummonSnapshot snapshot() { return SummonManager.getInstance().getSummonSnapshot(world, id); }
}
