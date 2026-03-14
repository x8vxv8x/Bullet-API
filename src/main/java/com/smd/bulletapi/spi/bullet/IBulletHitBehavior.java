package com.smd.bulletapi.spi.bullet;

import com.smd.bulletapi.api.annotation.SpiApi;
import com.smd.bulletapi.common.CollisionContext;

@SpiApi
public interface IBulletHitBehavior {
    void onHit(CollisionContext context);
}
