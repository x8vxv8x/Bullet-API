package com.smd.bulletapi.common.summon.behavior;

import com.smd.bulletapi.api.annotation.SpiApi;
import com.smd.bulletapi.common.summon.SummonContext;
import net.minecraft.util.math.Vec3d;

@SpiApi
public interface IFormationStrategy {
    Vec3d getAnchorPosition(SummonContext context);
}
