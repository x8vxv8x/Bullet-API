package com.smd.bulletapi.common.summon.behavior;

import com.smd.bulletapi.api.annotation.SpiApi;
import com.smd.bulletapi.api.summon.SummonCommand;
import com.smd.bulletapi.common.summon.SummonContext;

@SpiApi
public interface ISummonCommandHandler {
    boolean supportsCommand(String commandId);

    boolean handleCommand(SummonContext context, SummonCommand command);
}
