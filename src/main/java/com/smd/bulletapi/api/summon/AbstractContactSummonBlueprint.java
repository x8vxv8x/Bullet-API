package com.smd.bulletapi.api.summon;

import com.smd.bulletapi.common.summon.SummonDefinition;
import com.smd.bulletapi.common.summon.behavior.impl.NearestHostileSelector;
import com.smd.bulletapi.common.summon.behavior.impl.RingFormationStrategy;

public abstract class AbstractContactSummonBlueprint extends AbstractSummonBlueprint {
    protected AbstractContactSummonBlueprint(String id) {
        super(id);
    }

    @Override
    protected void configureBase(SummonDefinition.Builder builder) {
        builder.targetSelector(new NearestHostileSelector())
                .formationStrategy(new RingFormationStrategy())
                .attackPattern(null);
    }
}
