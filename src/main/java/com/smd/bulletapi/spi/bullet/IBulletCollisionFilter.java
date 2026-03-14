package com.smd.bulletapi.spi.bullet;

import com.smd.bulletapi.api.annotation.SpiApi;
import com.smd.bulletapi.api.runtime.IBulletActor;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

@SpiApi
public interface IBulletCollisionFilter {
    boolean canCollide(World world, IBulletActor bullet, EntityLivingBase entity);
}
