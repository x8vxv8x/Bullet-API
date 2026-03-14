package com.smd.bulletapi.common.summon.behavior;

import com.smd.bulletapi.api.annotation.SpiApi;
import com.smd.bulletapi.common.summon.SummonContext;
import net.minecraft.entity.EntityLivingBase;

@SpiApi
public interface ISummonTargetSelector {
    EntityLivingBase selectTarget(SummonContext context);
}
