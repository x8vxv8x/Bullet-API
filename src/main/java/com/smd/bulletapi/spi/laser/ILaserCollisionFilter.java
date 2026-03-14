package com.smd.bulletapi.spi.laser;

import com.smd.bulletapi.api.annotation.SpiApi;
import com.smd.bulletapi.api.runtime.ILaserActor;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

@SpiApi
public interface ILaserCollisionFilter {
    boolean canCollide(World world, ILaserActor laser, EntityLivingBase entity);
}
