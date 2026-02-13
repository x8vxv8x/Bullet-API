package com.smd.bulletapi.common.collision;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class AABBShape implements ICollisionShape {
    private final double width, height;

    public AABBShape(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public boolean intersects(Vec3d bulletPos, Entity entity) {
        AxisAlignedBB bulletBox = new AxisAlignedBB(
                bulletPos.x - width/2, bulletPos.y - height/2, bulletPos.z - width/2,
                bulletPos.x + width/2, bulletPos.y + height/2, bulletPos.z + width/2
        );
        return bulletBox.intersects(entity.getEntityBoundingBox());
    }

    @Override
    public boolean intersects(Entity entity) {
        return true;
    }
}
