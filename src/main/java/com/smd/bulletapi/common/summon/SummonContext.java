package com.smd.bulletapi.common.summon;

import com.smd.bulletapi.server.summon.SummonBullet;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SummonContext {
    public final SummonManager manager;
    public final World world;
    public final SummonBullet summon;
    public final EntityLivingBase owner;
    public final SummonDefinition definition;
    public final long worldTick;
    private EntityLivingBase target;

    public SummonContext(SummonManager manager, World world, SummonBullet summon,
                         EntityLivingBase owner, SummonDefinition definition,
                         long worldTick, EntityLivingBase target) {
        this.manager = manager;
        this.world = world;
        this.summon = summon;
        this.owner = owner;
        this.definition = definition;
        this.worldTick = worldTick;
        this.target = target;
    }

    public EntityLivingBase getTarget() {
        return target;
    }

    public void setTarget(EntityLivingBase target) {
        this.target = target;
        summon.setTarget(target);
    }

    public Vec3d getOwnerCenter() {
        return owner.getPositionVector().add(0, owner.getEyeHeight() * 0.7, 0);
    }

    public Vec3d getTargetCenter() {
        return target == null ? null : target.getPositionVector().add(0, target.height * 0.6, 0);
    }

    public int getOwnedSummonCount() {
        return Math.max(1, manager.getOwnedSummons(owner.getUniqueID()).size());
    }
}
