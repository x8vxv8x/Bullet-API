package com.smd.bulletapi.event.lifecycle;

import com.smd.bulletapi.api.annotation.PublicApi;

@PublicApi
public enum LifecycleRemoveReason {
    API_REQUEST,
    EXPIRED,
    WORLD_UNLOAD,
    OWNER_LOST,
    PLAYER_LOGOUT,
    DIMENSION_CHANGE,
    SLOT_RECONCILE
}
