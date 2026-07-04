package com.smd.bulletapi.api;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.handle.SummonHandle;
import com.smd.bulletapi.api.summon.SummonType;
import com.smd.bulletapi.common.summon.SummonManager;
import com.smd.bulletapi.common.summon.SummonRegistry;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

@PublicApi
public final class SummonApi {
    private SummonApi() {}

    public static SummonHandle handle(World world, int id) {
        return new SummonHandle(world, id);
    }

    public static int spawn(World world, EntityLivingBase owner, String typeId) {
        return SummonManager.getInstance().spawnSummon(world, owner, typeId);
    }

    public static int spawn(World world, EntityLivingBase owner, String typeId, Vec3d position) {
        return SummonManager.getInstance().spawnSummon(world, owner, typeId, position);
    }

    public static int getPlayerMaxSlots(EntityPlayer player) {
        return SummonManager.getInstance().getSlotManager().getMaxSlots(player);
    }

    public static int getPlayerUsedSlots(EntityPlayer player) {
        return SummonManager.getInstance().getSlotManager().getUsedSlots(player);
    }

    public static void setPlayerMaxSlots(EntityPlayer player, int slots) {
        SummonManager.getInstance().setPlayerMaxSlots(player, slots);
    }

    public static void registerType(SummonType type) {
        SummonRegistry.register(type);
    }

    public static SummonType getType(String id) {
        return SummonRegistry.get(id);
    }
}
