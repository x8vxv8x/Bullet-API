package com.smd.bulletapi.event;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.runtime.ILaserActor;
import com.smd.bulletapi.common.LaserCollisionContext;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
@PublicApi
public class LaserCollisionEvent extends WorldEvent {
    private final ILaserActor laser;
    private final EntityLivingBase hitEntity;
    private final LaserCollisionContext context;

    public LaserCollisionEvent(World world, ILaserActor laser, EntityLivingBase hitEntity, LaserCollisionContext context) {
        super(world);
        this.laser = laser;
        this.hitEntity = hitEntity;
        this.context = context;
    }

    public ILaserActor getLaser() { return laser; }
    public EntityLivingBase getHitEntity() { return hitEntity; }
    public LaserCollisionContext getContext() { return context; }
}
