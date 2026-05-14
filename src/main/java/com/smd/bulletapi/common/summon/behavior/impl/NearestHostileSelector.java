package com.smd.bulletapi.common.summon.behavior.impl;

import com.smd.bulletapi.common.summon.SummonContext;
import com.smd.bulletapi.common.summon.behavior.ISummonTargetSelector;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class NearestHostileSelector implements ISummonTargetSelector {
    @Override
    public EntityLivingBase selectTarget(SummonContext context) {
        Vec3d center = context.summon.getPosition();
        double range = context.definition.getFollowRange();
        AxisAlignedBB box = new AxisAlignedBB(center.x, center.y, center.z, center.x, center.y, center.z).grow(range);
        List<EntityLivingBase> candidates = context.world.getEntitiesWithinAABB(EntityLivingBase.class, box);

        EntityLivingBase best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (EntityLivingBase candidate : candidates) {
            if (!isValidTarget(context, candidate)) {
                continue;
            }
            double distSq = candidate.getDistanceSq(center.x, center.y, center.z);
            if (distSq < bestDistSq) {
                best = candidate;
                bestDistSq = distSq;
            }
        }
        return best;
    }

    private boolean isValidTarget(SummonContext context, EntityLivingBase entity) {
        if (entity == null || entity.isDead || entity == context.owner) {
            return false;
        }
        if (entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.disableDamage) {
            return false;
        }
        if (entity.getDistanceSq(context.owner) > context.definition.getFollowRange() * context.definition.getFollowRange()) {
            return false;
        }
        return entity instanceof IMob;
    }
}
