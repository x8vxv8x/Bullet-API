package com.smd.bulletapi.api.summon;

import com.smd.bulletapi.api.Battlefield;
import com.smd.bulletapi.api.annotation.SpiApi;
import com.smd.bulletapi.common.summon.SummonState;
import com.smd.bulletapi.common.summon.SummonTargetSource;
import com.smd.bulletapi.server.summon.SummonBullet;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

@SpiApi
public abstract class AbstractSummonEntity extends SummonBullet {
    protected AbstractSummonEntity(int id, SummonType type, EntityLivingBase owner, Vec3d position,
                                   int formationIndex, long spawnTick) {
        super(id, type, owner, position, formationIndex, spawnTick);
    }

    public abstract void tickServer(World world, EntityLivingBase owner, EntityLivingBase currentTarget);

    public void onBodyCollision(World world, EntityLivingBase target) {
    }

    public EntityLivingBase selectTarget(World world, EntityLivingBase owner) {
        return findNearestHostile(world, owner, getSpec().getFollowRange());
    }

    public boolean acceptsExternalTarget(World world, EntityLivingBase owner, EntityLivingBase target) {
        return target == null || !target.isDead;
    }

    public final EntityLivingBase sanitizeTarget(World world, EntityLivingBase owner) {
        EntityLivingBase target = getTarget(world);
        if (target == null || target.isDead) {
            clearTarget();
            return null;
        }
        double followRange = getSpec().getFollowRange();
        if (owner != null && target.getDistanceSq(owner) > followRange * followRange) {
            clearTarget();
            return null;
        }
        return target;
    }

    protected final EntityLivingBase acquireTarget(World world, EntityLivingBase owner) {
        EntityLivingBase target = selectTarget(world, owner);
        if (target == null) {
            clearTarget();
            return null;
        }
        setTarget(target, SummonTargetSource.AUTO);
        return target;
    }

    protected final boolean isOutsideLeash(EntityLivingBase owner) {
        if (owner == null) {
            return false;
        }
        double leashRange = getSpec().getLeashRange();
        double dx = getPosX() - owner.posX;
        double dy = getPosY() - owner.posY;
        double dz = getPosZ() - owner.posZ;
        return dx * dx + dy * dy + dz * dz > leashRange * leashRange;
    }

    protected final Vec3d getOwnerCenter(EntityLivingBase owner) {
        return owner.getPositionVector().add(0.0D, owner.getEyeHeight() * 0.7D, 0.0D);
    }

    protected final Vec3d getTargetCenter(EntityLivingBase target) {
        return target == null ? null : target.getPositionVector().add(0.0D, target.height * 0.6D, 0.0D);
    }

    protected final int getOwnedSummonCount(World world) {
        if (getOwnerId() == null) {
            return 1;
        }
        List<com.smd.bulletapi.api.snapshot.SummonSnapshot> owned = Battlefield.of(world).summons().ownedBy(getOwnerId());
        return Math.max(1, owned.size());
    }

    protected final EntityLivingBase findNearestHostile(World world, EntityLivingBase owner, double range) {
        if (world == null || owner == null || range <= 0.0D) {
            return null;
        }
        AxisAlignedBB box = owner.getEntityBoundingBox().grow(range);
        List<EntityLivingBase> candidates = world.getEntitiesWithinAABB(EntityLivingBase.class, box);
        EntityLivingBase nearest = null;
        double nearestSq = range * range;
        for (EntityLivingBase candidate : candidates) {
            if (candidate == null || candidate.isDead || candidate == owner) {
                continue;
            }
            if (candidate instanceof EntityPlayer && ((EntityPlayer) candidate).capabilities.disableDamage) {
                continue;
            }
            if (owner.isOnSameTeam(candidate)) {
                continue;
            }
            double distanceSq = candidate.getDistanceSq(owner);
            if (distanceSq > nearestSq) {
                continue;
            }
            nearestSq = distanceSq;
            nearest = candidate;
        }
        return nearest;
    }

    protected final Vec3d getRingAnchor(EntityLivingBase owner, double radius, double height, double angularSpeed) {
        int count = Math.max(1, getOwnedSummonCount(owner.world));
        double angle = owner.world.getTotalWorldTime() * angularSpeed + (Math.PI * 2.0D * getFormationIndex() / count);
        Vec3d center = getOwnerCenter(owner);
        return center.add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
    }

    protected final void moveToward(Vec3d desiredPosition, double baseSpeed, double maxSpeedMultiplier, double damping) {
        Vec3d current = getPosition();
        Vec3d toDesired = desiredPosition.subtract(current);
        if (toDesired.lengthSquared() < 1.0E-6D) {
            setVelocity(getVelocity().scale(damping));
            return;
        }

        double distance = toDesired.length();
        double speed = Math.min(
                baseSpeed + distance * 0.02D,
                Math.max(baseSpeed, baseSpeed * maxSpeedMultiplier)
        );
        Vec3d desiredVelocity = toDesired.normalize().scale(speed);
        Vec3d currentVelocity = getVelocity();
        double accel = getSpec().getAcceleration();
        setVelocity(currentVelocity.scale(1.0D - accel).add(desiredVelocity.scale(accel)));
    }

    protected final void moveToward(Vec3d desiredPosition, double speed, double accel) {
        Vec3d current = getPosition();
        Vec3d toDesired = desiredPosition.subtract(current);
        if (toDesired.lengthSquared() < 1.0E-6D) {
            return;
        }
        Vec3d desiredVelocity = toDesired.normalize().scale(speed);
        Vec3d currentVelocity = getVelocity();
        setVelocity(currentVelocity.scale(1.0D - accel).add(desiredVelocity.scale(accel)));
    }

    protected final void followOwnerOrbit(EntityLivingBase owner) {
        Vec3d ownerCenter = getOwnerCenter(owner);
        if (isOutsideLeash(owner)) {
            setState(SummonState.RETURNING);
            moveToward(ownerCenter, Math.max(getSpec().getMoveSpeed() * 1.5D, 0.45D), 3.0D, 0.65D);
            return;
        }

        Vec3d anchor = getRingAnchor(owner, getSpec().getIdleRadius(), getSpec().getIdleHeight(), 0.12D);
        setState(SummonState.FOLLOW_OWNER);
        moveToward(anchor, getSpec().getMoveSpeed(), 2.0D, 0.7D);
    }
}
