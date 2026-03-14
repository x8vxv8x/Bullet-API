package com.smd.bulletapi.api;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.builder.SummonBuilder;
import com.smd.bulletapi.api.handle.SummonHandle;
import com.smd.bulletapi.api.snapshot.SummonSnapshot;
import com.smd.bulletapi.api.summon.AbstractSummonBlueprint;
import com.smd.bulletapi.common.summon.SummonDefinition;
import com.smd.bulletapi.common.summon.SummonManager;
import com.smd.bulletapi.common.summon.SummonRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

@PublicApi
public final class SummonApi {
    private SummonApi() {}

    public static SummonBuilder builder(World world) {
        return new SummonBuilder(world);
    }

    public static SummonHandle handle(World world, int id) {
        return new SummonHandle(world, id);
    }

    public static void remove(World world, int id) {
        SummonManager.getInstance().removeSummon(world, id);
    }

    public static int getCount(World world) {
        return SummonManager.getInstance().getSummonCount(world);
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

    public static void registerDefinition(SummonDefinition definition) {
        SummonRegistry.register(definition);
    }

    public static void registerBlueprint(AbstractSummonBlueprint blueprint) {
        SummonRegistry.register(blueprint);
    }

    public static AbstractSummonBlueprint getBlueprint(String id) {
        return SummonRegistry.getBlueprint(id);
    }

    public static SummonDefinition getDefinition(String id) {
        return SummonRegistry.get(id);
    }

    public static boolean exists(World world, int id) {
        return SummonManager.getInstance().hasSummon(world, id);
    }

    public static SummonSnapshot snapshot(World world, int id) {
        return SummonManager.getInstance().getSummonSnapshot(world, id);
    }
}
