package com.smd.bulletapi.common.summon.blueprint;

import com.smd.bulletapi.common.summon.SummonDefinition;
import com.smd.bulletapi.common.summon.behavior.impl.NearestHostileSelector;
import com.smd.bulletapi.common.summon.behavior.impl.OrbitOwnerMoveController;
import com.smd.bulletapi.common.summon.behavior.impl.RingFormationStrategy;

public abstract class AbstractOrbitingSummonBlueprint extends AbstractSummonBlueprint {
    protected AbstractOrbitingSummonBlueprint(String id) {
        super(id);
    }

    @Override
    protected void configureBase(SummonDefinition.Builder builder) {
        builder.targetSelector(new NearestHostileSelector())
                .formationStrategy(new RingFormationStrategy())
                .moveController(new OrbitOwnerMoveController());
    }
}
