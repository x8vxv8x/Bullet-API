package com.smd.bulletapi.event;

import com.smd.bulletapi.common.LaserCollisionContext;
import com.smd.bulletapi.server.Laser;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
public class LaserCollisionEvent extends WorldEvent {
    private final Laser laser;
    private final EntityLivingBase hitEntity;
    private final LaserCollisionContext context;

    public LaserCollisionEvent(World world, Laser laser, EntityLivingBase hitEntity, LaserCollisionContext context) {
        super(world);
        this.laser = laser;
        this.hitEntity = hitEntity;
        this.context = context;
    }

    public Laser getLaser() { return laser; }
    public EntityLivingBase getHitEntity() { return hitEntity; }
    public LaserCollisionContext getContext() { return context; }
}
