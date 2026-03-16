package com.smd.bulletapi.api.snapshot;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.common.AttackSourceInfo;
import net.minecraft.util.math.Vec3d;

@PublicApi
public final class LaserSnapshot {
    public final int id;
    public final Vec3d start;
    public final Vec3d direction;
    public final double length;
    public final float thickness;
    public final int life;
    public final float damage;
    public final boolean onlyPlayer;
    public final boolean penetrate;
    public final boolean blockStops;
    public final AttackSourceInfo attackSource;

    public LaserSnapshot(int id, Vec3d start, Vec3d direction, double length,
                         float thickness, int life, float damage,
                         boolean onlyPlayer, boolean penetrate,
                         boolean blockStops, AttackSourceInfo attackSource) {
        this.id = id;
        this.start = start;
        this.direction = direction;
        this.length = length;
        this.thickness = thickness;
        this.life = life;
        this.damage = damage;
        this.onlyPlayer = onlyPlayer;
        this.penetrate = penetrate;
        this.blockStops = blockStops;
        this.attackSource = attackSource;
    }

    @Override
    public String toString() {
        return "LaserSnapshot{id=" + id + ", start=" + start + ", direction=" + direction
                + ", length=" + length + ", thickness=" + thickness + ", life=" + life
                + ", damage=" + damage + ", onlyPlayer=" + onlyPlayer + ", penetrate=" + penetrate
                + ", blockStops=" + blockStops + ", attackSource=" + attackSource + "}";
    }
}
