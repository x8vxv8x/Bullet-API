package com.smd.bulletapi.common.collision;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class SphereShape implements ICollisionShape {
    private final double radius;

    public SphereShape(double radius) { this.radius = radius; }

    @Override
    public boolean intersects(Vec3d shapePos, Entity target) {
        // 如果只想对 LivingBase 生效，保留 instanceof 判断
        if (!(target instanceof EntityLivingBase)) return false;
        AxisAlignedBB box = target.getEntityBoundingBox();
        double closestX = Math.max(box.minX, Math.min(shapePos.x, box.maxX));
        double closestY = Math.max(box.minY, Math.min(shapePos.y, box.maxY));
        double closestZ = Math.max(box.minZ, Math.min(shapePos.z, box.maxZ));
        double dx = shapePos.x - closestX;
        double dy = shapePos.y - closestY;
        double dz = shapePos.z - closestZ;
        return (dx * dx + dy * dy + dz * dz) <= radius * radius;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        if (entity instanceof EntityPlayer) {
            return !((EntityPlayer) entity).isCreative();
        }
        return entity instanceof EntityLivingBase;
    }
}