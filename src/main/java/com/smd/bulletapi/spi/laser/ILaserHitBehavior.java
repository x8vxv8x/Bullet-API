package com.smd.bulletapi.spi.laser;

import com.smd.bulletapi.api.annotation.SpiApi;
import com.smd.bulletapi.common.LaserCollisionContext;

@SpiApi
public interface ILaserHitBehavior {
    void onHit(LaserCollisionContext context);
}
