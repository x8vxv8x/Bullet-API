package com.smd.bulletapi.api.summon;

import com.smd.bulletapi.api.annotation.SpiApi;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@SpiApi
public abstract class SummonType {
    private final String id;
    private final SummonSpec spec;

    protected SummonType(String id, SummonSpec spec) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Summon type id must not be empty");
        }
        this.id = id;
        this.spec = spec == null ? new SummonSpec() : spec.copy();
    }

    public final String getId() {
        return id;
    }

    public final SummonSpec getSpec() {
        return spec.copy();
    }

    public abstract AbstractSummonEntity createEntity(int id, World world, EntityLivingBase owner,
                                                      Vec3d position, int formationIndex, long spawnTick);
}
