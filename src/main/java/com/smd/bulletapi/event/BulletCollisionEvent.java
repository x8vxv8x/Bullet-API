package com.smd.bulletapi.event;

import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.server.Bullet;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
public class BulletCollisionEvent extends WorldEvent {
    private final Bullet bullet;
    private final Entity hitEntity;
    private final CollisionContext context;

    public BulletCollisionEvent(World world, Bullet bullet, Entity hitEntity, CollisionContext context) {
        super(world);
        this.bullet = bullet;
        this.hitEntity = hitEntity;
        this.context = context;
    }

    public Bullet getBullet() { return bullet; }
    public Entity getHitEntity() { return hitEntity; }
    public CollisionContext getContext() { return context; }
}
