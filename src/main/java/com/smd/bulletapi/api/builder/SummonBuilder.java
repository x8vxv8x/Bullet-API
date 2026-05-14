package com.smd.bulletapi.api.builder;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.handle.SummonHandle;
import com.smd.bulletapi.api.summon.AbstractSummonBlueprint;
import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.common.summon.SummonCommandResponsePolicy;
import com.smd.bulletapi.common.summon.SummonDefinition;
import com.smd.bulletapi.common.summon.SummonManager;
import com.smd.bulletapi.common.summon.SummonRegistry;
import com.smd.bulletapi.common.summon.behavior.IFormationStrategy;
import com.smd.bulletapi.common.summon.behavior.ISummonAttackPattern;
import com.smd.bulletapi.common.summon.behavior.ISummonMoveController;
import com.smd.bulletapi.common.summon.behavior.ISummonTargetSelector;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@PublicApi
public class SummonBuilder {
    private final World world;
    private EntityLivingBase owner;
    private Vec3d position;
    private String definitionId;
    private SummonDefinition definition;

    public SummonBuilder(World world) {
        this.world = world;
    }

    public SummonBuilder owner(EntityLivingBase owner) {
        this.owner = owner;
        return this;
    }

    public SummonBuilder position(Vec3d position) {
        this.position = position;
        return this;
    }

    public SummonBuilder definition(String definitionId) {
        this.definitionId = definitionId;
        this.definition = null;
        return this;
    }

    public SummonBuilder definition(SummonDefinition definition) {
        this.definition = definition == null ? null : definition.copy();
        this.definitionId = definition == null ? null : definition.getId();
        return this;
    }

    public SummonBuilder definition(AbstractSummonBlueprint blueprint) {
        if (blueprint == null) {
            this.definition = null;
            this.definitionId = null;
        } else {
            this.definition = blueprint.createDefinition();
            this.definitionId = blueprint.getId();
        }
        return this;
    }

    public SummonBuilder slotCost(int slotCost) {
        resolveDefinitionForMutation().slotCost(slotCost);
        return this;
    }

    public SummonBuilder life(int life) {
        resolveDefinitionForMutation().life(life);
        return this;
    }

    public SummonBuilder damage(float damage) {
        resolveDefinitionForMutation().damage(damage);
        return this;
    }

    public SummonBuilder texture(String texture) {
        resolveDefinitionForMutation().texture(texture);
        return this;
    }

    public SummonBuilder color(int color) {
        resolveDefinitionForMutation().color(color);
        return this;
    }

    public SummonBuilder size(float size) {
        resolveDefinitionForMutation().size(size);
        return this;
    }

    public SummonBuilder rendererType(String rendererType) {
        resolveDefinitionForMutation().rendererType(rendererType);
        return this;
    }

    public SummonBuilder collisionShape(ICollisionShape collisionShape) {
        resolveDefinitionForMutation().collisionShape(collisionShape);
        return this;
    }

    public SummonBuilder followRange(double range) {
        resolveDefinitionForMutation().followRange(range);
        return this;
    }

    public SummonBuilder attackRange(double range) {
        resolveDefinitionForMutation().attackRange(range);
        return this;
    }

    public SummonBuilder leashRange(double range) {
        resolveDefinitionForMutation().leashRange(range);
        return this;
    }

    public SummonBuilder moveSpeed(double speed) {
        resolveDefinitionForMutation().moveSpeed(speed);
        return this;
    }

    public SummonBuilder acceleration(double acceleration) {
        resolveDefinitionForMutation().acceleration(acceleration);
        return this;
    }

    public SummonBuilder idleHeight(double idleHeight) {
        resolveDefinitionForMutation().idleHeight(idleHeight);
        return this;
    }

    public SummonBuilder idleRadius(double idleRadius) {
        resolveDefinitionForMutation().idleRadius(idleRadius);
        return this;
    }

    public SummonBuilder retargetIntervalTicks(int ticks) {
        resolveDefinitionForMutation().retargetIntervalTicks(ticks);
        return this;
    }

    public SummonBuilder syncIntervalTicks(int ticks) {
        resolveDefinitionForMutation().syncIntervalTicks(ticks);
        return this;
    }

    public SummonBuilder bodyCollisionIntervalTicks(int ticks) {
        resolveDefinitionForMutation().bodyCollisionIntervalTicks(ticks);
        return this;
    }

    public SummonBuilder commandResponsePolicy(SummonCommandResponsePolicy commandResponsePolicy) {
        resolveDefinitionForMutation().commandResponsePolicy(commandResponsePolicy);
        return this;
    }

    public SummonBuilder targetSelector(ISummonTargetSelector selector) {
        resolveDefinitionForMutation().targetSelector(selector);
        return this;
    }

    public SummonBuilder moveController(ISummonMoveController controller) {
        resolveDefinitionForMutation().moveController(controller);
        return this;
    }

    public SummonBuilder attackPattern(ISummonAttackPattern pattern) {
        resolveDefinitionForMutation().attackPattern(pattern);
        return this;
    }

    public SummonBuilder formationStrategy(IFormationStrategy strategy) {
        resolveDefinitionForMutation().formationStrategy(strategy);
        return this;
    }

    public SummonBuilder set(String key, String value) {
        resolveDefinitionForMutation().set(key, value);
        return this;
    }

    public SummonBuilder set(String key, int value) {
        resolveDefinitionForMutation().set(key, value);
        return this;
    }

    public SummonBuilder set(String key, float value) {
        resolveDefinitionForMutation().set(key, value);
        return this;
    }

    public SummonBuilder set(String key, boolean value) {
        resolveDefinitionForMutation().set(key, value);
        return this;
    }

    public int spawn() {
        return spawnHandle().getId();
    }

    public SummonHandle spawnHandle() {
        if (world.isRemote) {
            throw new IllegalStateException("Cannot spawn summon on client side");
        }
        if (owner == null) {
            throw new IllegalStateException("Summon owner must be set");
        }
        int id = SummonManager.getInstance().spawnSummon(world, owner, resolveDefinition(), position);
        return new SummonHandle(world, id);
    }

    private SummonDefinition resolveDefinition() {
        SummonDefinition resolved = definition == null
                ? (definitionId == null ? null : SummonRegistry.get(definitionId))
                : definition.copy();
        if (resolved == null) {
            throw new IllegalStateException("Summon definition must be set");
        }
        return resolved;
    }

    private SummonDefinition resolveDefinitionForMutation() {
        if (definition == null) {
            definition = definitionId == null ? null : SummonRegistry.get(definitionId);
        }
        if (definition == null) {
            throw new IllegalStateException("Summon definition must be set before overriding properties");
        }
        return definition;
    }
}
