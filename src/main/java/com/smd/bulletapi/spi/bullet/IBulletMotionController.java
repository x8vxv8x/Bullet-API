package com.smd.bulletapi.spi.bullet;

import com.smd.bulletapi.api.annotation.SpiApi;
import com.smd.bulletapi.api.runtime.IBulletActor;
import net.minecraft.world.World;

@SpiApi
public interface IBulletMotionController {
    void tick(World world, IBulletActor bullet);
}
