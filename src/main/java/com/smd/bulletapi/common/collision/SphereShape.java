package com.smd.bulletapi.common.collision;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class SphereShape implements ICollisionShape {
    private final double radius;

    public SphereShape(double radius) {
        this.radius = radius;
    }

    @Override
    public boolean intersects(Entity entity) {
        // 球心到实体包围盒最近点的距离 ≤ 半径
        Vec3d center = entity.getPositionVector(); // 实际应传入弹幕位置，由调用方提供
        // 此处仅定义形状，具体检测需传入弹幕位置，因此在调用时需提供位置参数
        // 为简化，我们将 intersects 定义为 (Vec3d pos, Entity e)
        throw new UnsupportedOperationException("Use intersects(Vec3d, Entity) instead");
    }

    // 实际使用的方法（在碰撞检测时调用）
    public boolean intersects(Vec3d bulletPos, Entity entity) {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        double closestX = Math.max(box.minX, Math.min(bulletPos.x, box.maxX));
        double closestY = Math.max(box.minY, Math.min(bulletPos.y, box.maxY));
        double closestZ = Math.max(box.minZ, Math.min(bulletPos.z, box.maxZ));
        double dx = bulletPos.x - closestX;
        double dy = bulletPos.y - closestY;
        double dz = bulletPos.z - closestZ;
        return (dx * dx + dy * dy + dz * dz) <= radius * radius;
    }
}
