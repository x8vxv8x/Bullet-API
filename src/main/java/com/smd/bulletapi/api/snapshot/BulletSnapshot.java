package com.smd.bulletapi.api.snapshot;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.common.AttackSourceInfo;
import net.minecraft.util.math.Vec3d;

@PublicApi
public final class BulletSnapshot {
    public final int id;
    public final Vec3d position;
    public final Vec3d velocity;
    public final int life;
    public final float damage;
    public final boolean onlyPlayer;
    public final AttackSourceInfo attackSource;

    public BulletSnapshot(int id, Vec3d position, Vec3d velocity, int life, float damage,
                          boolean onlyPlayer, AttackSourceInfo attackSource) {
        this.id = id;
        this.position = position;
        this.velocity = velocity;
        this.life = life;
        this.damage = damage;
        this.onlyPlayer = onlyPlayer;
        this.attackSource = attackSource;
    }

    @Override
    public String toString() {
        return "BulletSnapshot{id=" + id + ", position=" + position + ", velocity=" + velocity
                + ", life=" + life + ", damage=" + damage + ", onlyPlayer=" + onlyPlayer
                + ", attackSource=" + attackSource + "}";
    }
}
