package com.smd.bulletapi.client;

import com.smd.bulletapi.api.annotation.InternalApi;
import net.minecraft.nbt.NBTTagCompound;
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
                        NBTTagCompound customData) {
        super(id, position, velocity, maxLife, damage, texture, color, size, rendererType, customData);
        this.targetPositionX = position.x;
        this.targetPositionY = position.y;
        this.targetPositionZ = position.z;
    }

    @Override
    public void tick() {
        if (dead) return;
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
        if (life <= 0) dead = true;
    }

    @Override
    public void applySnapshot(Vec3d position, Vec3d velocity, int life) {
        applyUpdate(position, velocity, Integer.valueOf(life));
    }

    @Override
    public void applyUpdate(Vec3d position, Vec3d velocity, Integer life) {
        if (position != null) {
            double dx = targetPositionX - position.x;
            double dy = targetPositionY - position.y;
            double dz = targetPositionZ - position.z;
            targetPositionX = position.x;
            targetPositionY = position.y;
            targetPositionZ = position.z;

            if (dx * dx + dy * dy + dz * dz >= SNAP_DISTANCE_SQ) {
                prevPositionX = position.x;
                prevPositionY = position.y;
                prevPositionZ = position.z;
                positionX = position.x;
                positionY = position.y;
                positionZ = position.z;
            }
        }
        if (velocity != null) {
            velocityX = velocity.x;
            velocityY = velocity.y;
            velocityZ = velocity.z;
        }
        if (life != null) {
            this.life = life;
            this.dead = life <= 0;
        }
    }
}
