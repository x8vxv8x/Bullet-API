package com.smd.bulletapi.event.lifecycle;

import com.smd.bulletapi.api.annotation.PublicApi;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.UUID;

@PublicApi
public class SummonSlotChangedEvent extends Event {
    private final EntityPlayer player;
    private final UUID playerId;
    private final int previousUsedSlots;
    private final int newUsedSlots;
    private final int previousMaxSlots;
    private final int newMaxSlots;

    public SummonSlotChangedEvent(EntityPlayer player, UUID playerId, int previousUsedSlots, int newUsedSlots,
                                  int previousMaxSlots, int newMaxSlots) {
        this.player = player;
        this.playerId = playerId;
        this.previousUsedSlots = previousUsedSlots;
        this.newUsedSlots = newUsedSlots;
        this.previousMaxSlots = previousMaxSlots;
        this.newMaxSlots = newMaxSlots;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getPreviousUsedSlots() {
        return previousUsedSlots;
    }

    public int getNewUsedSlots() {
        return newUsedSlots;
    }

    public int getPreviousMaxSlots() {
        return previousMaxSlots;
    }

    public int getNewMaxSlots() {
        return newMaxSlots;
    }
}
