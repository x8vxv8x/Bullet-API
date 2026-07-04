package com.smd.bulletapi.api.summon;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.common.data.DataPayload;

@PublicApi
public class SummonSpec {
    private int life = 6000;
    private float damage = 2.0f;
    private int slotCost = 1;
    private String texture;
    private int color = 0xFFFFFF;
    private float size = 0.75f;
    private String rendererType = "billboard";
    private DataPayload customData = new DataPayload();
    private ICollisionShape collisionShape;
    private double followRange = 24.0D;
    private double attackRange = 16.0D;
    private double leashRange = 24.0D;
    private double moveSpeed = 0.25D;
    private double acceleration = 0.12D;
    private double idleHeight = 1.5D;
    private double idleRadius = 1.8D;
    private int retargetIntervalTicks = 10;
    private int syncIntervalTicks = 2;
    private int bodyCollisionIntervalTicks = 8;

    public SummonSpec copy() {
        SummonSpec copy = new SummonSpec();
        copy.life = life;
        copy.damage = damage;
        copy.slotCost = slotCost;
        copy.texture = texture;
        copy.color = color;
        copy.size = size;
        copy.rendererType = rendererType;
        copy.customData = customData == null ? new DataPayload() : customData.copy();
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
        return copy;
    }

    public int getLife() { return life; }
    public SummonSpec life(int life) { this.life = life; return this; }
    public float getDamage() { return damage; }
    public SummonSpec damage(float damage) { this.damage = damage; return this; }
    public int getSlotCost() { return slotCost; }
    public SummonSpec slotCost(int slotCost) { this.slotCost = slotCost; return this; }
    public String getTexture() { return texture; }
    public SummonSpec texture(String texture) { this.texture = texture; return this; }
    public int getColor() { return color; }
    public SummonSpec color(int color) { this.color = color; return this; }
    public float getSize() { return size; }
    public SummonSpec size(float size) { this.size = size; return this; }
    public String getRendererType() { return rendererType; }
    public SummonSpec rendererType(String rendererType) { this.rendererType = rendererType; return this; }
    public DataPayload getCustomData() { return customData == null ? new DataPayload() : customData.copy(); }
    public SummonSpec customData(DataPayload customData) {
        this.customData = customData == null ? new DataPayload() : customData.copy();
        return this;
    }
    public ICollisionShape getCollisionShape() { return collisionShape; }
    public SummonSpec collisionShape(ICollisionShape collisionShape) { this.collisionShape = collisionShape; return this; }
    public double getFollowRange() { return followRange; }
    public SummonSpec followRange(double followRange) { this.followRange = followRange; return this; }
    public double getAttackRange() { return attackRange; }
    public SummonSpec attackRange(double attackRange) { this.attackRange = attackRange; return this; }
    public double getLeashRange() { return leashRange; }
    public SummonSpec leashRange(double leashRange) { this.leashRange = leashRange; return this; }
    public double getMoveSpeed() { return moveSpeed; }
    public SummonSpec moveSpeed(double moveSpeed) { this.moveSpeed = moveSpeed; return this; }
    public double getAcceleration() { return acceleration; }
    public SummonSpec acceleration(double acceleration) { this.acceleration = acceleration; return this; }
    public double getIdleHeight() { return idleHeight; }
    public SummonSpec idleHeight(double idleHeight) { this.idleHeight = idleHeight; return this; }
    public double getIdleRadius() { return idleRadius; }
    public SummonSpec idleRadius(double idleRadius) { this.idleRadius = idleRadius; return this; }
    public int getRetargetIntervalTicks() { return retargetIntervalTicks; }
    public SummonSpec retargetIntervalTicks(int retargetIntervalTicks) { this.retargetIntervalTicks = retargetIntervalTicks; return this; }
    public int getSyncIntervalTicks() { return syncIntervalTicks; }
    public SummonSpec syncIntervalTicks(int syncIntervalTicks) { this.syncIntervalTicks = syncIntervalTicks; return this; }
    public int getBodyCollisionIntervalTicks() { return bodyCollisionIntervalTicks; }
    public SummonSpec bodyCollisionIntervalTicks(int bodyCollisionIntervalTicks) {
        this.bodyCollisionIntervalTicks = Math.max(0, bodyCollisionIntervalTicks);
        return this;
    }

    public SummonSpec set(String key, String value) {
        customData.setString(key, value);
        return this;
    }

    public SummonSpec set(String key, int value) {
        customData.setInteger(key, value);
        return this;
    }

    public SummonSpec set(String key, float value) {
        customData.setFloat(key, value);
        return this;
    }

    public SummonSpec set(String key, boolean value) {
        customData.setBoolean(key, value);
        return this;
    }
}
