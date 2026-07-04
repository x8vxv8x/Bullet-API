package com.smd.bulletapi.common.runtime.state;

import com.smd.bulletapi.api.annotation.InternalApi;
import net.minecraft.util.math.Vec3d;

@InternalApi
public final class MotionState3D {
    private double positionX;
    private double positionY;
    private double positionZ;
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private int life;
    private boolean dead;

    public MotionState3D(Vec3d position, Vec3d velocity, int life) {
        this(
                position == null ? 0.0D : position.x,
                position == null ? 0.0D : position.y,
                position == null ? 0.0D : position.z,
                velocity == null ? 0.0D : velocity.x,
                velocity == null ? 0.0D : velocity.y,
                velocity == null ? 0.0D : velocity.z,
                life
        );
    }

    public MotionState3D(double positionX, double positionY, double positionZ,
                         double velocityX, double velocityY, double velocityZ,
                         int life) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.positionZ = positionZ;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.life = life;
    }

    public void tickLinear() {
        positionX += velocityX;
        positionY += velocityY;
        positionZ += velocityZ;
        life--;
        if (life <= 0) {
            dead = true;
        }
    }

    public Vec3d getPosition() {
        return new Vec3d(positionX, positionY, positionZ);
    }

    public double getPositionX() {
        return positionX;
    }

    public double getPositionY() {
        return positionY;
    }

    public double getPositionZ() {
        return positionZ;
    }

    public void setPosition(Vec3d position) {
        setPosition(position.x, position.y, position.z);
    }

    public void setPosition(double x, double y, double z) {
        this.positionX = x;
        this.positionY = y;
        this.positionZ = z;
    }

    public Vec3d getVelocity() {
        return new Vec3d(velocityX, velocityY, velocityZ);
    }

    public double getVelocityX() {
        return velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public double getVelocityZ() {
        return velocityZ;
    }

    public void setVelocity(Vec3d velocity) {
        setVelocity(velocity.x, velocity.y, velocity.z);
    }

    public void setVelocity(double x, double y, double z) {
        this.velocityX = x;
        this.velocityY = y;
        this.velocityZ = z;
    }

    public int getLife() {
        return life;
    }

    public void setLife(int life) {
        this.life = life;
    }

    public boolean isDead() {
        return dead;
    }

    public void markDead() {
        this.dead = true;
    }
}
