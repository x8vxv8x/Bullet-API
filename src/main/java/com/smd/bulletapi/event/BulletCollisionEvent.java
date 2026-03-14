package com.smd.bulletapi.event;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.runtime.IBulletActor;
import com.smd.bulletapi.common.CollisionContext;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
@PublicApi
public class BulletCollisionEvent extends WorldEvent {
    private final IBulletActor bullet;
    private final Entity hitEntity;
    private final CollisionContext context;

    public BulletCollisionEvent(World world, IBulletActor bullet, Entity hitEntity, CollisionContext context) {
        super(world);
        this.bullet = bullet;
        this.hitEntity = hitEntity;
        this.context = context;
    }

    public IBulletActor getBullet() { return bullet; }
    public Entity getHitEntity() { return hitEntity; }
    public CollisionContext getContext() { return context; }
}
