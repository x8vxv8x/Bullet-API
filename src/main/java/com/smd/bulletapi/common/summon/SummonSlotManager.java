package com.smd.bulletapi.common.summon;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SummonSlotManager {
    private static final String ROOT_KEY = "bulletapi_summon";
    private static final String MAX_SLOTS_KEY = "max_slots";
    private static final int DEFAULT_MAX_SLOTS = 3;

    private static final Map<UUID, Integer> USED_SLOTS = new ConcurrentHashMap<>();

    public int getMaxSlots(EntityPlayer player) {
        NBTTagCompound root = getRootTag(player);
        if (!root.hasKey(MAX_SLOTS_KEY)) {
            root.setInteger(MAX_SLOTS_KEY, DEFAULT_MAX_SLOTS);
        }
        return root.getInteger(MAX_SLOTS_KEY);
    }

    public void setMaxSlots(EntityPlayer player, int slots) {
        getRootTag(player).setInteger(MAX_SLOTS_KEY, Math.max(0, slots));
    }

    public int getUsedSlots(EntityPlayer player) {
        return USED_SLOTS.getOrDefault(player.getUniqueID(), 0);
    }

    public boolean reserve(EntityPlayer player, int cost) {
        int actualCost = Math.max(0, cost);
        int used = getUsedSlots(player);
        if (used + actualCost > getMaxSlots(player)) {
            return false;
        }
        USED_SLOTS.put(player.getUniqueID(), used + actualCost);
        return true;
    }

    public void release(EntityPlayer player, int cost) {
        release(player.getUniqueID(), cost);
    }

    public void release(UUID playerId, int cost) {
        int actualCost = Math.max(0, cost);
        int next = Math.max(0, USED_SLOTS.getOrDefault(playerId, 0) - actualCost);
        if (next == 0) {
            USED_SLOTS.remove(playerId);
        } else {
            USED_SLOTS.put(playerId, next);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        NBTTagCompound original = getRootTag(event.getOriginal());
        if (original.hasKey(MAX_SLOTS_KEY)) {
            getRootTag(event.getEntityPlayer()).setInteger(MAX_SLOTS_KEY, original.getInteger(MAX_SLOTS_KEY));
        }
        USED_SLOTS.remove(event.getOriginal().getUniqueID());
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerLoggedOutEvent event) {
        USED_SLOTS.remove(event.player.getUniqueID());
    }

    private static NBTTagCompound getRootTag(EntityPlayer player) {
        NBTTagCompound entityData = player.getEntityData();
        if (!entityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        NBTTagCompound persisted = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!persisted.hasKey(ROOT_KEY)) {
            persisted.setTag(ROOT_KEY, new NBTTagCompound());
        }
        return persisted.getCompoundTag(ROOT_KEY);
    }
}
