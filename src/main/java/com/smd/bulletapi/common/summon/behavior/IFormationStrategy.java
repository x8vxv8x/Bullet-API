package com.smd.bulletapi.common.summon.behavior;

import com.smd.bulletapi.common.summon.SummonContext;
import net.minecraft.util.math.Vec3d;

public interface IFormationStrategy {
    Vec3d getAnchorPosition(SummonContext context);
}
