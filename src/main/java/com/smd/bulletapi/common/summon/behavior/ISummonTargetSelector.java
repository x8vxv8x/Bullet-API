package com.smd.bulletapi.common.summon.behavior;

import com.smd.bulletapi.common.summon.SummonContext;
import net.minecraft.entity.EntityLivingBase;

public interface ISummonTargetSelector {
    EntityLivingBase selectTarget(SummonContext context);
}
