package com.smd.bulletapi.common.collision;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;

public interface ICollisionShape {
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
     * 统一的碰撞检查入口：先快速过滤，再精确检测。
     * 外部调用者只需使用此方法，无需关心内部过滤逻辑。
     */
    default boolean checkCollision(Vec3d shapePos, Entity entity) {
        return canCollideWith(entity) && intersects(shapePos, entity);
    }
}
