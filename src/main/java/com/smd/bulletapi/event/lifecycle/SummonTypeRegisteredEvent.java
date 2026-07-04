package com.smd.bulletapi.event.lifecycle;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.summon.SummonType;
import net.minecraftforge.fml.common.eventhandler.Event;

@PublicApi
public class SummonTypeRegisteredEvent extends Event {
    private final SummonType type;

    public SummonTypeRegisteredEvent(SummonType type) {
        this.type = type;
    }

    public SummonType getType() {
        return type;
    }
}
