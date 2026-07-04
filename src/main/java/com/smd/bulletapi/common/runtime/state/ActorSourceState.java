package com.smd.bulletapi.common.runtime.state;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.common.AttackSourceInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

@InternalApi
public final class ActorSourceState {
    private final boolean onlyPlayer;
    private final EntityLivingBase shooter;
    private final ItemStack shooterHeldItem;
    private final AttackSourceInfo attackSourceInfo;

    public ActorSourceState(boolean onlyPlayer, EntityLivingBase shooter, ItemStack shooterHeldItem, AttackSourceInfo attackSourceInfo) {
        this.onlyPlayer = onlyPlayer;
        this.shooter = shooter;
        this.shooterHeldItem = shooterHeldItem == null ? null : shooterHeldItem.copy();
        this.attackSourceInfo = attackSourceInfo == null ? AttackSourceInfo.normal() : attackSourceInfo;
    }

    public boolean isOnlyPlayer() {
        return onlyPlayer;
    }

    public EntityLivingBase getShooter() {
        return shooter;
    }

    public ItemStack getShooterHeldItem() {
        return shooterHeldItem;
    }

    public AttackSourceInfo getAttackSourceInfo() {
        return attackSourceInfo;
    }
}
