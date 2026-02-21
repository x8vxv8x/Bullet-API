package com.smd.bulletapi.common.collision;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class CollisionHelper {
    public static boolean checkCollision(ICollisionShape shape, Vec3d bulletPos, Entity entity) {
        return shape != null && shape.checkCollision(bulletPos, entity);
    }
}
