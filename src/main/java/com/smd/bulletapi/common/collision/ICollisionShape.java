package com.smd.bulletapi.common.collision;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;

public interface ICollisionShape {
    boolean intersects(Entity entity);
}
