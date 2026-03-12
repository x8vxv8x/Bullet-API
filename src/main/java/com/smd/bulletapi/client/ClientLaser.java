package com.smd.bulletapi.client;

import com.smd.bulletapi.client.render.ILaserRenderer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

public class ClientLaser {
    private final int id;
    private Vec3d start;
    private Vec3d prevStart;
    private Vec3d targetStart;
    private Vec3d direction;
    private Vec3d prevDirection;
    private Vec3d targetDirection;
    private double length;
    private double prevLength;
    private double targetLength;
    private final float thickness;
    private final int color;
    private final String rendererType;
    private final NBTTagCompound customData;
    private ILaserRenderer renderer;
    private boolean initialized;

    public ClientLaser(int id, long tick, Vec3d start, Vec3d direction, double length,
                       float thickness, int color, String rendererType,
                       NBTTagCompound customData) {
        this.id = id;
        this.start = start;
        this.prevStart = start;
        this.targetStart = start;
        this.direction = direction == null ? new Vec3d(0, 0, 1) : direction;
        this.prevDirection = this.direction;
        this.targetDirection = this.direction;
        this.length = length;
        this.prevLength = length;
        this.targetLength = length;
        this.thickness = thickness;
        this.color = color;
        this.rendererType = rendererType;
        this.customData = customData == null ? new NBTTagCompound() : customData;
        this.initialized = true;
    }

    public void update(long tick, Vec3d start, Vec3d direction, double length) {
        if (!initialized) {
            this.start = start;
            this.prevStart = start;
            this.targetStart = start;
            this.direction = direction == null ? new Vec3d(0, 0, 1) : direction;
            this.prevDirection = this.direction;
            this.targetDirection = this.direction;
            this.length = length;
            this.prevLength = length;
            this.targetLength = length;
            this.initialized = true;
            return;
        }
        this.targetStart = start;
        this.targetDirection = direction == null ? new Vec3d(0, 0, 1) : direction;
        this.targetLength = length;
    }

    public void tick() {
        if (!initialized) return;
        prevStart = start;
        prevDirection = direction;
        prevLength = length;

        double alpha = 0.35;
        start = lerp(start, targetStart, alpha);
        direction = lerp(prevDirection, targetDirection, alpha);
        if (direction.lengthSquared() < 1.0E-6) {
            direction = targetDirection;
        } else {
            direction = direction.normalize();
        }
        length = prevLength + (targetLength - prevLength) * alpha;
    }

    public int getId() { return id; }
    public Vec3d getStart() { return start; }
    public Vec3d getDirection() { return direction; }
    public double getLength() { return length; }
    public Vec3d getEnd() { return start.add(direction.scale(length)); }

    public Vec3d getRenderStart(float partialTicks) {
        return lerp(prevStart, start, partialTicks);
    }

    public Vec3d getRenderDirection(float partialTicks) {
        Vec3d d = lerp(prevDirection, direction, partialTicks);
        if (d.lengthSquared() < 1.0E-6) return direction;
        return d.normalize();
    }

    public double getRenderLength(float partialTicks) {
        return prevLength + (length - prevLength) * partialTicks;
    }

    public Vec3d getRenderEnd(float partialTicks) {
        Vec3d s = getRenderStart(partialTicks);
        Vec3d d = getRenderDirection(partialTicks);
        return s.add(d.scale(getRenderLength(partialTicks)));
    }
    public float getThickness() { return thickness; }
    public int getColor() { return color; }
    public String getRendererType() { return rendererType; }
    public NBTTagCompound getCustomData() { return customData; }
    public ILaserRenderer getRenderer() { return renderer; }
    public void setRenderer(ILaserRenderer renderer) { this.renderer = renderer; }

    private static Vec3d lerp(Vec3d a, Vec3d b, double t) {
        if (a == null) return b;
        if (b == null) return a;
        double x = a.x + (b.x - a.x) * t;
        double y = a.y + (b.y - a.y) * t;
        double z = a.z + (b.z - a.z) * t;
        return new Vec3d(x, y, z);
    }
}
