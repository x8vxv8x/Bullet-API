package com.smd.bulletapi.client;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.client.render.ILaserRenderer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

@InternalApi
public class ClientLaser {
    private final int id;
    private Vec3d start;
    private Vec3d prevStart;
    private Vec3d direction;
    private Vec3d prevDirection;
    private double length;
    private double prevLength;
    private final float thickness;
    private final int color;
    private final String rendererType;
    private final NBTTagCompound customData;
    private ILaserRenderer renderer;
    private boolean initialized;
    private long lastSnapshotTick;

    public ClientLaser(int id, long tick, Vec3d start, Vec3d direction, double length,
                       float thickness, int color, String rendererType,
                       NBTTagCompound customData) {
        this.id = id;
        this.start = start;
        this.prevStart = start;
        this.direction = normalizeOrDefault(direction);
        this.prevDirection = this.direction;
        this.length = length;
        this.prevLength = length;
        this.thickness = thickness;
        this.color = color;
        this.rendererType = rendererType;
        this.customData = customData == null ? new NBTTagCompound() : customData;
        this.initialized = true;
        this.lastSnapshotTick = tick;
    }

    public void update(long tick, Vec3d start, Vec3d direction, double length) {
        if (tick < lastSnapshotTick) {
            return;
        }
        if (!initialized) {
            this.start = start;
            this.prevStart = start;
            this.direction = normalizeOrDefault(direction);
            this.prevDirection = this.direction;
            this.length = length;
            this.prevLength = length;
            this.initialized = true;
            this.lastSnapshotTick = tick;
            return;
        }

        this.prevStart = this.start;
        this.prevDirection = this.direction;
        this.prevLength = this.length;

        this.start = start;
        this.direction = normalizeOrDefault(direction);
        this.length = length;
        this.lastSnapshotTick = tick;
    }

    public void tick() {
        if (!initialized) return;
        prevStart = start;
        prevDirection = direction;
        prevLength = length;
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

    private static Vec3d normalizeOrDefault(Vec3d direction) {
        if (direction == null || direction.lengthSquared() < 1.0E-6) {
            return new Vec3d(0, 0, 1);
        }
        return direction.normalize();
    }

    private static Vec3d lerp(Vec3d a, Vec3d b, double t) {
        if (a == null) return b;
        if (b == null) return a;
        double x = a.x + (b.x - a.x) * t;
        double y = a.y + (b.y - a.y) * t;
        double z = a.z + (b.z - a.z) * t;
        return new Vec3d(x, y, z);
    }
}
