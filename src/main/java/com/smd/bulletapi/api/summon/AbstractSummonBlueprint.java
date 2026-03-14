package com.smd.bulletapi.api.summon;

import com.smd.bulletapi.api.annotation.SpiApi;
import com.smd.bulletapi.common.summon.SummonDefinition;

@SpiApi
public abstract class AbstractSummonBlueprint {
    private final String id;

    protected AbstractSummonBlueprint(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Summon blueprint id must not be empty");
        }
        this.id = id;
    }

    public final String getId() {
        return id;
    }

    public final SummonDefinition createDefinition() {
        SummonDefinition.Builder builder = SummonDefinition.builder(id);
        configureBase(builder);
        configure(builder);
        return builder.build();
    }

    protected void configureBase(SummonDefinition.Builder builder) {
    }

    protected abstract void configure(SummonDefinition.Builder builder);
}
