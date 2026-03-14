package com.smd.bulletapi.common.summon.behavior;

import com.smd.bulletapi.api.annotation.SpiApi;
import com.smd.bulletapi.common.summon.SummonContext;

@SpiApi
public interface ISummonMoveController {
    void tickMovement(SummonContext context);
}
