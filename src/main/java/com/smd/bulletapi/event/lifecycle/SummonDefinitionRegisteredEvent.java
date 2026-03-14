package com.smd.bulletapi.event.lifecycle;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.common.summon.SummonDefinition;
import net.minecraftforge.fml.common.eventhandler.Event;

@PublicApi
public class SummonDefinitionRegisteredEvent extends Event {
    private final SummonDefinition definition;

    public SummonDefinitionRegisteredEvent(SummonDefinition definition) {
        this.definition = definition == null ? null : definition.copy();
    }

    public SummonDefinition getDefinition() {
        return definition == null ? null : definition.copy();
    }
}
