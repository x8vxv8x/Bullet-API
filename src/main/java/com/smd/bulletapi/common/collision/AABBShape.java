package com.smd.bulletapi.common.collision;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class AABBShape implements ICollisionShape {
    private final double width, height; // 宽度（X/Z方向），高度（Y方向）

    public AABBShape(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public double getBroadphaseRadius() {
        double halfW = width / 2.0;
        double halfH = height / 2.0;
        return Math.max(halfW, halfH);
    }

    @Override
    public boolean intersects(Vec3d shapePos, Entity target) {
        return intersects(shapePos.x, shapePos.y, shapePos.z, target);
    }

    @Override
    public boolean intersects(double x, double y, double z, Entity target) {
        if (!(target instanceof EntityLivingBase)) {
            return false;
        }
        AxisAlignedBB box = target.getEntityBoundingBox();
        double halfW = width / 2.0;
        double halfH = height / 2.0;
        double minX = x - halfW;
        double maxX = x + halfW;
        double minY = y - halfH;
        double maxY = y + halfH;
        double minZ = z - halfW;
        double maxZ = z + halfW;
        return box.intersects(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
