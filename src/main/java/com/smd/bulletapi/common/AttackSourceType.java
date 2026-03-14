package com.smd.bulletapi.common;

public enum AttackSourceType {
    NORMAL_BULLET,
    SUMMON_BODY,
    SUMMON_CHILD_BULLET,
    SUMMON_CHILD_LASER;

    public boolean isSummonSource() {
        return this == SUMMON_BODY || this == SUMMON_CHILD_BULLET || this == SUMMON_CHILD_LASER;
    }
}
