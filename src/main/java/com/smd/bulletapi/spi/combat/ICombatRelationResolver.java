package com.smd.bulletapi.spi.combat;

import com.smd.bulletapi.api.annotation.SpiApi;
import com.smd.bulletapi.api.runtime.IBulletActor;
import com.smd.bulletapi.api.runtime.ILaserActor;
import com.smd.bulletapi.api.runtime.ISummonActor;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

@SpiApi
public interface ICombatRelationResolver {
    default CombatRelation canBulletHit(World world, IBulletActor bullet, EntityLivingBase entity) {
        return CombatRelation.DEFAULT;
    }

    default CombatRelation canLaserHit(World world, ILaserActor laser, EntityLivingBase entity) {
        return CombatRelation.DEFAULT;
    }

    default CombatRelation canSummonBodyHit(World world, ISummonActor summon, EntityLivingBase entity) {
        return CombatRelation.DEFAULT;
    }
}
