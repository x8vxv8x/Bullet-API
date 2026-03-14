package com.smd.bulletapi.common.summon;

import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.common.summon.behavior.IFormationStrategy;
import com.smd.bulletapi.common.summon.behavior.ISummonAttackPattern;
import com.smd.bulletapi.common.summon.behavior.ISummonMoveController;
import com.smd.bulletapi.common.summon.behavior.ISummonTargetSelector;
import net.minecraft.nbt.NBTTagCompound;

public class SummonDefinition {
    private final String id;
    private int life = 6000;
    private float damage = 2.0f;
    private int slotCost = 1;
    private String texture;
    private int color = 0xFFFFFF;
    private float size = 0.75f;
    private String rendererType = "billboard";
    private NBTTagCompound customData = new NBTTagCompound();
    private ICollisionShape collisionShape;
    private double followRange = 24.0;
    private double attackRange = 16.0;
    private double leashRange = 24.0;
    private double moveSpeed = 0.25;
    private double acceleration = 0.12;
    private double idleHeight = 1.5;
    private double idleRadius = 1.8;
    private int retargetIntervalTicks = 10;
    private int syncIntervalTicks = 2;
    private int bodyCollisionIntervalTicks = 8;
    private ISummonTargetSelector targetSelector;
    private ISummonMoveController moveController;
    private ISummonAttackPattern attackPattern;
    private IFormationStrategy formationStrategy;

    public SummonDefinition(String id) {
        this.id = id;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public SummonDefinition copy() {
        SummonDefinition copy = new SummonDefinition(id);
        copy.life = life;
        copy.damage = damage;
        copy.slotCost = slotCost;
        copy.texture = texture;
        copy.color = color;
        copy.size = size;
        copy.rendererType = rendererType;
        copy.customData = customData == null ? new NBTTagCompound() : customData.copy();
        copy.collisionShape = collisionShape;
        copy.followRange = followRange;
        copy.attackRange = attackRange;
        copy.leashRange = leashRange;
        copy.moveSpeed = moveSpeed;
        copy.acceleration = acceleration;
        copy.idleHeight = idleHeight;
        copy.idleRadius = idleRadius;
        copy.retargetIntervalTicks = retargetIntervalTicks;
        copy.syncIntervalTicks = syncIntervalTicks;
        copy.bodyCollisionIntervalTicks = bodyCollisionIntervalTicks;
        copy.targetSelector = targetSelector;
        copy.moveController = moveController;
        copy.attackPattern = attackPattern;
        copy.formationStrategy = formationStrategy;
        return copy;
    }

    public String getId() { return id; }
    public int getLife() { return life; }
    public SummonDefinition life(int life) { this.life = life; return this; }
    public float getDamage() { return damage; }
    public SummonDefinition damage(float damage) { this.damage = damage; return this; }
    public int getSlotCost() { return slotCost; }
    public SummonDefinition slotCost(int slotCost) { this.slotCost = slotCost; return this; }
    public String getTexture() { return texture; }
    public SummonDefinition texture(String texture) { this.texture = texture; return this; }
    public int getColor() { return color; }
    public SummonDefinition color(int color) { this.color = color; return this; }
    public float getSize() { return size; }
    public SummonDefinition size(float size) { this.size = size; return this; }
    public String getRendererType() { return rendererType; }
    public SummonDefinition rendererType(String rendererType) { this.rendererType = rendererType; return this; }
    public NBTTagCompound getCustomData() { return customData; }
    public SummonDefinition customData(NBTTagCompound customData) {
        this.customData = customData == null ? new NBTTagCompound() : customData;
        return this;
    }
    public ICollisionShape getCollisionShape() { return collisionShape; }
    public SummonDefinition collisionShape(ICollisionShape collisionShape) { this.collisionShape = collisionShape; return this; }
    public double getFollowRange() { return followRange; }
    public SummonDefinition followRange(double followRange) { this.followRange = followRange; return this; }
    public double getAttackRange() { return attackRange; }
    public SummonDefinition attackRange(double attackRange) { this.attackRange = attackRange; return this; }
    public double getLeashRange() { return leashRange; }
    public SummonDefinition leashRange(double leashRange) { this.leashRange = leashRange; return this; }
    public double getMoveSpeed() { return moveSpeed; }
    public SummonDefinition moveSpeed(double moveSpeed) { this.moveSpeed = moveSpeed; return this; }
    public double getAcceleration() { return acceleration; }
    public SummonDefinition acceleration(double acceleration) { this.acceleration = acceleration; return this; }
    public double getIdleHeight() { return idleHeight; }
    public SummonDefinition idleHeight(double idleHeight) { this.idleHeight = idleHeight; return this; }
    public double getIdleRadius() { return idleRadius; }
    public SummonDefinition idleRadius(double idleRadius) { this.idleRadius = idleRadius; return this; }
    public int getRetargetIntervalTicks() { return retargetIntervalTicks; }
    public SummonDefinition retargetIntervalTicks(int retargetIntervalTicks) { this.retargetIntervalTicks = retargetIntervalTicks; return this; }
    public int getSyncIntervalTicks() { return syncIntervalTicks; }
    public SummonDefinition syncIntervalTicks(int syncIntervalTicks) { this.syncIntervalTicks = syncIntervalTicks; return this; }
    public int getBodyCollisionIntervalTicks() { return bodyCollisionIntervalTicks; }
    public SummonDefinition bodyCollisionIntervalTicks(int bodyCollisionIntervalTicks) {
        this.bodyCollisionIntervalTicks = Math.max(0, bodyCollisionIntervalTicks);
        return this;
    }
    public ISummonTargetSelector getTargetSelector() { return targetSelector; }
    public SummonDefinition targetSelector(ISummonTargetSelector targetSelector) { this.targetSelector = targetSelector; return this; }
    public ISummonMoveController getMoveController() { return moveController; }
    public SummonDefinition moveController(ISummonMoveController moveController) { this.moveController = moveController; return this; }
    public ISummonAttackPattern getAttackPattern() { return attackPattern; }
    public SummonDefinition attackPattern(ISummonAttackPattern attackPattern) { this.attackPattern = attackPattern; return this; }
    public IFormationStrategy getFormationStrategy() { return formationStrategy; }
    public SummonDefinition formationStrategy(IFormationStrategy formationStrategy) { this.formationStrategy = formationStrategy; return this; }

    public SummonDefinition set(String key, String value) {
        customData.setString(key, value);
        return this;
    }

    public SummonDefinition set(String key, int value) {
        customData.setInteger(key, value);
        return this;
    }

    public SummonDefinition set(String key, float value) {
        customData.setFloat(key, value);
        return this;
    }

    public SummonDefinition set(String key, boolean value) {
        customData.setBoolean(key, value);
        return this;
    }

    public static final class Builder {
        private final SummonDefinition definition;

        private Builder(String id) {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Summon definition id must not be empty");
            }
            this.definition = new SummonDefinition(id);
        }

        private Builder(SummonDefinition source) {
            if (source == null) {
                throw new IllegalArgumentException("Source summon definition must not be null");
            }
            this.definition = source.copy();
        }

        public Builder life(int life) {
            definition.life(life);
            return this;
        }

        public Builder damage(float damage) {
            definition.damage(damage);
            return this;
        }

        public Builder slotCost(int slotCost) {
            definition.slotCost(slotCost);
            return this;
        }

        public Builder texture(String texture) {
            definition.texture(texture);
            return this;
        }

        public Builder color(int color) {
            definition.color(color);
            return this;
        }

        public Builder size(float size) {
            definition.size(size);
            return this;
        }

        public Builder rendererType(String rendererType) {
            definition.rendererType(rendererType);
            return this;
        }

        public Builder customData(NBTTagCompound customData) {
            definition.customData(customData == null ? new NBTTagCompound() : customData.copy());
            return this;
        }

        public Builder collisionShape(ICollisionShape collisionShape) {
            definition.collisionShape(collisionShape);
            return this;
        }

        public Builder followRange(double followRange) {
            definition.followRange(followRange);
            return this;
        }

        public Builder attackRange(double attackRange) {
            definition.attackRange(attackRange);
            return this;
        }

        public Builder leashRange(double leashRange) {
            definition.leashRange(leashRange);
            return this;
        }

        public Builder moveSpeed(double moveSpeed) {
            definition.moveSpeed(moveSpeed);
            return this;
        }

        public Builder acceleration(double acceleration) {
            definition.acceleration(acceleration);
            return this;
        }

        public Builder idleHeight(double idleHeight) {
            definition.idleHeight(idleHeight);
            return this;
        }

        public Builder idleRadius(double idleRadius) {
            definition.idleRadius(idleRadius);
            return this;
        }

        public Builder retargetIntervalTicks(int ticks) {
            definition.retargetIntervalTicks(ticks);
            return this;
        }

        public Builder syncIntervalTicks(int ticks) {
            definition.syncIntervalTicks(ticks);
            return this;
        }

        public Builder bodyCollisionIntervalTicks(int ticks) {
            definition.bodyCollisionIntervalTicks(ticks);
            return this;
        }

        public Builder targetSelector(ISummonTargetSelector selector) {
            definition.targetSelector(selector);
            return this;
        }

        public Builder moveController(ISummonMoveController controller) {
            definition.moveController(controller);
            return this;
        }

        public Builder attackPattern(ISummonAttackPattern pattern) {
            definition.attackPattern(pattern);
            return this;
        }

        public Builder formationStrategy(IFormationStrategy formationStrategy) {
            definition.formationStrategy(formationStrategy);
            return this;
        }

        public Builder set(String key, String value) {
            definition.set(key, value);
            return this;
        }

        public Builder set(String key, int value) {
            definition.set(key, value);
            return this;
        }

        public Builder set(String key, float value) {
            definition.set(key, value);
            return this;
        }

        public Builder set(String key, boolean value) {
            definition.set(key, value);
            return this;
        }

        public Builder copyFrom(SummonDefinition source) {
            if (source == null) return this;
            SummonDefinition copy = source.copy();
            definition.life(copy.getLife());
            definition.damage(copy.getDamage());
            definition.slotCost(copy.getSlotCost());
            definition.texture(copy.getTexture());
            definition.color(copy.getColor());
            definition.size(copy.getSize());
            definition.rendererType(copy.getRendererType());
            definition.customData(copy.getCustomData());
            definition.collisionShape(copy.getCollisionShape());
            definition.followRange(copy.getFollowRange());
            definition.attackRange(copy.getAttackRange());
            definition.leashRange(copy.getLeashRange());
            definition.moveSpeed(copy.getMoveSpeed());
            definition.acceleration(copy.getAcceleration());
            definition.idleHeight(copy.getIdleHeight());
            definition.idleRadius(copy.getIdleRadius());
            definition.retargetIntervalTicks(copy.getRetargetIntervalTicks());
            definition.syncIntervalTicks(copy.getSyncIntervalTicks());
            definition.bodyCollisionIntervalTicks(copy.getBodyCollisionIntervalTicks());
            definition.targetSelector(copy.getTargetSelector());
            definition.moveController(copy.getMoveController());
            definition.attackPattern(copy.getAttackPattern());
            definition.formationStrategy(copy.getFormationStrategy());
            return this;
        }

        public SummonDefinition build() {
            return definition.copy();
        }
    }
}
