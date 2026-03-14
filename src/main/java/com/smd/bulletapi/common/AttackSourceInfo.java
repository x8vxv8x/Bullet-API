package com.smd.bulletapi.common;

import net.minecraft.nbt.NBTTagCompound;

import java.util.UUID;

public class AttackSourceInfo {
    public static final String TAG_SOURCE_TYPE = "bulletapi_source_type";
    public static final String TAG_OWNER_UUID = "bulletapi_source_owner_uuid";
    public static final String TAG_SUMMON_ID = "bulletapi_source_summon_id";
    public static final String TAG_SUMMON_DEF = "bulletapi_source_summon_def";

    private static final AttackSourceInfo NORMAL = new AttackSourceInfo(AttackSourceType.NORMAL_BULLET, null, -1, null);

    private final AttackSourceType type;
    private final UUID ownerId;
    private final int summonId;
    private final String summonDefinitionId;

    public AttackSourceInfo(AttackSourceType type, UUID ownerId, int summonId, String summonDefinitionId) {
        this.type = type == null ? AttackSourceType.NORMAL_BULLET : type;
        this.ownerId = ownerId;
        this.summonId = summonId;
        this.summonDefinitionId = summonDefinitionId;
    }

    public static AttackSourceInfo normal() {
        return NORMAL;
    }

    public static AttackSourceInfo summonBody(UUID ownerId, int summonId, String summonDefinitionId) {
        return new AttackSourceInfo(AttackSourceType.SUMMON_BODY, ownerId, summonId, summonDefinitionId);
    }

    public static AttackSourceInfo summonChildBullet(UUID ownerId, int summonId, String summonDefinitionId) {
        return new AttackSourceInfo(AttackSourceType.SUMMON_CHILD_BULLET, ownerId, summonId, summonDefinitionId);
    }

    public static AttackSourceInfo summonChildLaser(UUID ownerId, int summonId, String summonDefinitionId) {
        return new AttackSourceInfo(AttackSourceType.SUMMON_CHILD_LASER, ownerId, summonId, summonDefinitionId);
    }

    public AttackSourceType getType() {
        return type;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public int getSummonId() {
        return summonId;
    }

    public String getSummonDefinitionId() {
        return summonDefinitionId;
    }

    public boolean isSummonSource() {
        return type.isSummonSource();
    }

    public boolean isSummonBody() {
        return type == AttackSourceType.SUMMON_BODY;
    }

    public boolean isSummonChildBullet() {
        return type == AttackSourceType.SUMMON_CHILD_BULLET;
    }

    public boolean isSummonChildLaser() {
        return type == AttackSourceType.SUMMON_CHILD_LASER;
    }

    public static AttackSourceInfo fromTag(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(TAG_SOURCE_TYPE)) {
            return normal();
        }
        AttackSourceType type;
        try {
            type = AttackSourceType.valueOf(tag.getString(TAG_SOURCE_TYPE));
        } catch (IllegalArgumentException ex) {
            type = AttackSourceType.NORMAL_BULLET;
        }

        UUID ownerId = null;
        if (tag.hasKey(TAG_OWNER_UUID)) {
            try {
                ownerId = UUID.fromString(tag.getString(TAG_OWNER_UUID));
            } catch (IllegalArgumentException ignored) {
                ownerId = null;
            }
        }
        int summonId = tag.hasKey(TAG_SUMMON_ID) ? tag.getInteger(TAG_SUMMON_ID) : -1;
        String summonDefinitionId = tag.hasKey(TAG_SUMMON_DEF) ? tag.getString(TAG_SUMMON_DEF) : null;
        return new AttackSourceInfo(type, ownerId, summonId, summonDefinitionId);
    }

    public void writeToTag(NBTTagCompound tag) {
        if (tag == null) return;
        tag.setString(TAG_SOURCE_TYPE, type.name());
        if (ownerId != null) {
            tag.setString(TAG_OWNER_UUID, ownerId.toString());
        }
        if (summonId >= 0) {
            tag.setInteger(TAG_SUMMON_ID, summonId);
        }
        if (summonDefinitionId != null) {
            tag.setString(TAG_SUMMON_DEF, summonDefinitionId);
        }
    }
}
