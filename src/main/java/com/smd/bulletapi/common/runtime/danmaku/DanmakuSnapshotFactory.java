package com.smd.bulletapi.common.runtime.danmaku;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.api.snapshot.BulletSnapshot;
import com.smd.bulletapi.api.snapshot.LaserSnapshot;
import com.smd.bulletapi.common.runtime.RuntimeSnapshotFactory;
import com.smd.bulletapi.server.Bullet;
import com.smd.bulletapi.server.Laser;

@InternalApi
public final class DanmakuSnapshotFactory {
    private final RuntimeSnapshotFactory<Bullet, BulletSnapshot> bulletSnapshots =
            bullet -> new BulletSnapshot(
                    bullet.getId(),
                    bullet.getPosition(),
                    bullet.getVelocity(),
                    bullet.getLife(),
                    bullet.getDamage(),
                    bullet.isOnlyPlayer(),
                    bullet.getAttackSourceInfo()
            );

    private final RuntimeSnapshotFactory<Laser, LaserSnapshot> laserSnapshots =
            laser -> new LaserSnapshot(
                    laser.getId(),
                    laser.getStart(),
                    laser.getDirection(),
                    laser.getCurrentLength(),
                    laser.getThickness(),
                    laser.getLife(),
                    laser.getDamage(),
                    laser.isOnlyPlayer(),
                    laser.isPenetrate(),
                    laser.isBlockStops(),
                    laser.getAttackSourceInfo()
            );

    public BulletSnapshot createBulletSnapshot(Bullet bullet) {
        return bulletSnapshots.create(bullet);
    }

    public LaserSnapshot createLaserSnapshot(Laser laser) {
        return laserSnapshots.create(laser);
    }

    public RuntimeSnapshotFactory<Bullet, BulletSnapshot> bulletSnapshots() {
        return bulletSnapshots;
    }

    public RuntimeSnapshotFactory<Laser, LaserSnapshot> laserSnapshots() {
        return laserSnapshots;
    }
}
