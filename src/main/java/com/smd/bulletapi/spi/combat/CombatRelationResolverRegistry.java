package com.smd.bulletapi.spi.combat;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.runtime.IBulletActor;
import com.smd.bulletapi.api.runtime.ILaserActor;
import com.smd.bulletapi.api.runtime.ISummonActor;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@PublicApi
public final class CombatRelationResolverRegistry {
    private static final List<ICombatRelationResolver> RESOLVERS = new CopyOnWriteArrayList<>();

    private CombatRelationResolverRegistry() {}

    public static void register(ICombatRelationResolver resolver) {
        if (resolver != null) {
            RESOLVERS.add(resolver);
        }
    }

    public static void unregister(ICombatRelationResolver resolver) {
        RESOLVERS.remove(resolver);
    }

    public static CombatRelation resolveBullet(World world, IBulletActor bullet, EntityLivingBase entity) {
        CombatRelation result = CombatRelation.DEFAULT;
        for (ICombatRelationResolver resolver : RESOLVERS) {
            CombatRelation next = resolver.canBulletHit(world, bullet, entity);
            if (next == CombatRelation.DENY) {
                return CombatRelation.DENY;
            }
            if (next == CombatRelation.ALLOW) {
                result = CombatRelation.ALLOW;
            }
        }
        return result;
    }

    public static CombatRelation resolveLaser(World world, ILaserActor laser, EntityLivingBase entity) {
        CombatRelation result = CombatRelation.DEFAULT;
        for (ICombatRelationResolver resolver : RESOLVERS) {
            CombatRelation next = resolver.canLaserHit(world, laser, entity);
            if (next == CombatRelation.DENY) {
                return CombatRelation.DENY;
            }
            if (next == CombatRelation.ALLOW) {
                result = CombatRelation.ALLOW;
            }
        }
        return result;
    }

    public static CombatRelation resolveSummon(World world, ISummonActor summon, EntityLivingBase entity) {
        CombatRelation result = CombatRelation.DEFAULT;
        for (ICombatRelationResolver resolver : RESOLVERS) {
            CombatRelation next = resolver.canSummonBodyHit(world, summon, entity);
            if (next == CombatRelation.DENY) {
                return CombatRelation.DENY;
            }
            if (next == CombatRelation.ALLOW) {
                result = CombatRelation.ALLOW;
            }
        }
        return result;
    }
}
