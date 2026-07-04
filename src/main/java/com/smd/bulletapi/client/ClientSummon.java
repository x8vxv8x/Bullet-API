package com.smd.bulletapi.client;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.common.data.DataPayload;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

@InternalApi
public class ClientSummon extends ClientBullet {
    private static final double POSITION_BLEND = 0.35D;
    private static final double SNAP_DISTANCE_SQ = 4.0D * 4.0D;

    private double targetPositionX;
    private double targetPositionY;
    private double targetPositionZ;

    public ClientSummon(int id, Vec3d position, Vec3d velocity, int maxLife, float damage,
                        ResourceLocation texture, int color, float size, String rendererType,
                        DataPayload customData) {
        this(id, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z,
                maxLife, damage, texture, color, size, rendererType, customData);
    }

    public ClientSummon(int id, double positionX, double positionY, double positionZ,
                        double velocityX, double velocityY, double velocityZ,
                        int maxLife, float damage,
                        ResourceLocation texture, int color, float size, String rendererType,
                        DataPayload customData) {
        super(id, positionX, positionY, positionZ, velocityX, velocityY, velocityZ,
                maxLife, damage, texture, color, size, rendererType, customData);
        this.targetPositionX = positionX;
        this.targetPositionY = positionY;
        this.targetPositionZ = positionZ;
    }

    @Override
    public void tick() {
        if (dead) {
            return;
        }
        prevPositionX = positionX;
        prevPositionY = positionY;
        prevPositionZ = positionZ;

        targetPositionX += velocityX;
        targetPositionY += velocityY;
        targetPositionZ += velocityZ;

        positionX += (targetPositionX - positionX) * POSITION_BLEND;
        positionY += (targetPositionY - positionY) * POSITION_BLEND;
        positionZ += (targetPositionZ - positionZ) * POSITION_BLEND;

        life--;
        if (life <= 0) {
            dead = true;
        }
    }

    @Override
    public void applyUpdate(Vec3d position, Vec3d velocity, Integer life) {
        applyUpdate(position != null, position != null ? position.x : 0.0D, position != null ? position.y : 0.0D,
                position != null ? position.z : 0.0D, velocity != null, velocity != null ? velocity.x : 0.0D,
                velocity != null ? velocity.y : 0.0D, velocity != null ? velocity.z : 0.0D, life);
    }

    @Override
    public void applyUpdate(boolean hasPosition, double positionX, double positionY, double positionZ,
                            boolean hasVelocity, double velocityX, double velocityY, double velocityZ,
                            Integer life) {
        if (hasPosition) {
            double dx = targetPositionX - positionX;
            double dy = targetPositionY - positionY;
            double dz = targetPositionZ - positionZ;
            targetPositionX = positionX;
            targetPositionY = positionY;
            targetPositionZ = positionZ;

            if (dx * dx + dy * dy + dz * dz >= SNAP_DISTANCE_SQ) {
                prevPositionX = positionX;
                prevPositionY = positionY;
                prevPositionZ = positionZ;
                this.positionX = positionX;
                this.positionY = positionY;
                this.positionZ = positionZ;
            }
        }
        if (hasVelocity) {
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.velocityZ = velocityZ;
        }
        if (life != null) {
            this.life = life;
            this.dead = life <= 0;
        }
    }
}
