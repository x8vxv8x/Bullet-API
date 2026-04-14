package com.smd.bulletapi.common.summon;

import com.smd.bulletapi.api.annotation.PublicApi;

@PublicApi
public enum SummonCommandResponsePolicy {
    STRICT_LOCK,
    IGNORE_COMMAND,
    COMBAT_ONLY
}
