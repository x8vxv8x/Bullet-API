package com.smd.bulletapi.api;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.builder.BulletBuilder;
import com.smd.bulletapi.api.handle.BulletHandle;
import net.minecraft.world.World;

@PublicApi
public final class BulletApi {
    private BulletApi() {}

    public static BulletBuilder builder(World world) {
        return new BulletBuilder(world);
    }

    public static BulletHandle handle(World world, int id) {
        return new BulletHandle(world, id);
    }
}
