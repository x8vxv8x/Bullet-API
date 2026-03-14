package com.smd.bulletapi.api.summon;

import com.smd.bulletapi.api.annotation.PublicApi;
import net.minecraft.nbt.NBTTagCompound;

@PublicApi
public final class SummonCommand {
    public static final String FORCE_RETARGET = "bulletapi:force_retarget";
    public static final String FORCE_TARGET = "bulletapi:force_target";
    public static final String CLEAR_TARGET = "bulletapi:clear_target";
    public static final String RETURN_TO_OWNER = "bulletapi:return_to_owner";
    public static final String DESPAWN = "bulletapi:despawn";

    public static final String KEY_TARGET_ENTITY_ID = "target_entity_id";

    private final String commandId;
    private final NBTTagCompound payload;

    private SummonCommand(String commandId, NBTTagCompound payload) {
        if (commandId == null || commandId.trim().isEmpty()) {
            throw new IllegalArgumentException("Summon command id must not be empty");
        }
        this.commandId = commandId;
        this.payload = payload == null ? new NBTTagCompound() : payload.copy();
    }

    public static SummonCommand of(String commandId) {
        return new SummonCommand(commandId, new NBTTagCompound());
    }

    public static SummonCommand forceTarget(int entityId) {
        return of(FORCE_TARGET).withInt(KEY_TARGET_ENTITY_ID, entityId);
    }

    public String getCommandId() {
        return commandId;
    }

    public NBTTagCompound getPayload() {
        return payload.copy();
    }

    public boolean has(String key) {
        return payload.hasKey(key);
    }

    public int getInt(String key, int defaultValue) {
        return payload.hasKey(key) ? payload.getInteger(key) : defaultValue;
    }

    public String getString(String key, String defaultValue) {
        return payload.hasKey(key) ? payload.getString(key) : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return payload.hasKey(key) ? payload.getBoolean(key) : defaultValue;
    }

    public SummonCommand withInt(String key, int value) {
        payload.setInteger(key, value);
        return this;
    }

    public SummonCommand withString(String key, String value) {
        payload.setString(key, value);
        return this;
    }

    public SummonCommand withBoolean(String key, boolean value) {
        payload.setBoolean(key, value);
        return this;
    }

    public SummonCommand copy() {
        return new SummonCommand(commandId, payload);
    }
}
