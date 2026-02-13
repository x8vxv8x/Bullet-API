package com.smd.bulletapi.common.collision;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class CollisionHelper {
    public static boolean checkCollision(ICollisionShape shape, Vec3d bulletPos, Entity entity) {
        if (shape instanceof SphereShape) {
            return ((SphereShape) shape).intersects(bulletPos, entity);
        } else if (shape instanceof AABBShape) {
            return ((AABBShape) shape).intersects(bulletPos, entity);
        }
        return false;
    }
}
