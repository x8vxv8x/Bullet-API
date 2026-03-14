package com.smd.bulletapi.api;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.snapshot.BulletSnapshot;
import com.smd.bulletapi.api.snapshot.LaserSnapshot;
import com.smd.bulletapi.api.snapshot.SummonSnapshot;
import com.smd.bulletapi.common.DanmakuManager;
import com.smd.bulletapi.common.summon.SummonManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import java.util.UUID;

@PublicApi
public final class BattlefieldQueryApi {
    private BattlefieldQueryApi() {}

    public static int getBulletCount(World world) {
        return DanmakuManager.getInstance().getBulletCount(world);
    }

    public static int getLaserCount(World world) {
        return DanmakuManager.getInstance().getLaserCount(world);
    }

    public static int getSummonCount(World world) {
        return SummonManager.getInstance().getSummonCount(world);
    }

    public static int getOwnedSummonCount(UUID ownerId) {
        return SummonManager.getInstance().getOwnedSummons(ownerId).size();
    }

    public static int getPlayerMaxSlots(EntityPlayer player) {
        return SummonApi.getPlayerMaxSlots(player);
    }

    public static int getPlayerUsedSlots(EntityPlayer player) {
        return SummonApi.getPlayerUsedSlots(player);
    }

    public static BulletSnapshot getBulletSnapshot(World world, int id) {
        return DanmakuManager.getInstance().getBulletSnapshot(world, id);
    }

    public static LaserSnapshot getLaserSnapshot(World world, int id) {
        return DanmakuManager.getInstance().getLaserSnapshot(world, id);
    }

    public static SummonSnapshot getSummonSnapshot(World world, int id) {
        return SummonManager.getInstance().getSummonSnapshot(world, id);
    }
}
