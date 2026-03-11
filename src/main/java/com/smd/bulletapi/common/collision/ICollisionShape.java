package com.smd.bulletapi.common.collision;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;

public interface ICollisionShape {
    /**
     * broad-phase 粗筛半径（方块坐标单位）。
     * 返回 <= 0 表示调用方应回退到全量候选集合。
     */
    default double getBroadphaseRadius() {
        return 0.0D;
    }

    /**
     * 快速过滤：判断该形状是否可能与目标实体发生碰撞。
     * 默认实现只允许与 {@link net.minecraft.entity.EntityLivingBase} 碰撞。
     * 子类可覆盖此方法以添加更精细的过滤（如距离、队伍、无敌状态等）。
     */
    default boolean canCollideWith(Entity entity) {
        return entity instanceof EntityLivingBase;
    }

    /**
     * 精确几何检测（由具体形状实现）。
     * @param shapePos 形状的中心位置（弹幕当前位置）
     * @param entity   目标实体（已通过 canCollideWith 过滤）
     */
    boolean intersects(Vec3d shapePos, Entity entity);

    /**
     * 精确几何检测（坐标重载，避免热路径频繁创建 Vec3d）。
     * 自定义形状可按需覆盖以减少对象分配。
     */
    default boolean intersects(double x, double y, double z, Entity entity) {
        return intersects(new Vec3d(x, y, z), entity);
    }

    /**
     * 统一的碰撞检查入口：先快速过滤，再精确检测。
     * 外部调用者只需使用此方法，无需关心内部过滤逻辑。
     */
    default boolean checkCollision(Vec3d shapePos, Entity entity) {
        return canCollideWith(entity) && intersects(shapePos, entity);
    }

    /**
     * 统一碰撞检查入口（坐标重载）。
     */
    default boolean checkCollision(double x, double y, double z, Entity entity) {
        return canCollideWith(entity) && intersects(x, y, z, entity);
    }
}
