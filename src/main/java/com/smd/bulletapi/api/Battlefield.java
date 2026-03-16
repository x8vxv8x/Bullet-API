package com.smd.bulletapi.api;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.handle.BulletHandle;
import com.smd.bulletapi.api.handle.LaserHandle;
import com.smd.bulletapi.api.handle.SummonHandle;
import com.smd.bulletapi.api.snapshot.BulletSnapshot;
import com.smd.bulletapi.api.snapshot.LaserSnapshot;
import com.smd.bulletapi.api.snapshot.SummonSnapshot;
import com.smd.bulletapi.common.DanmakuManager;
import com.smd.bulletapi.common.summon.SummonManager;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

@PublicApi
public final class Battlefield {
    private final World world;
    private final Bullets bullets = new Bullets();
    private final Lasers lasers = new Lasers();
    private final Summons summons = new Summons();

    private Battlefield(World world) {
        if (world == null) {
            throw new IllegalArgumentException("World must not be null");
        }
        this.world = world;
    }

    public static Battlefield of(World world) {
        return new Battlefield(world);
    }

    public World getWorld() {
        return world;
    }

    public Bullets bullets() {
        return bullets;
    }

    public Lasers lasers() {
        return lasers;
    }

    public Summons summons() {
        return summons;
    }

    @PublicApi
    public final class Bullets {
        private Bullets() {}

        public int count() {
            return DanmakuManager.getInstance().getBulletCount(world);
        }

        public boolean contains(int id) {
            return DanmakuManager.getInstance().hasBullet(world, id);
        }

        public List<Integer> ids() {
            return DanmakuManager.getInstance().getBulletIds(world);
        }

        public List<BulletSnapshot> all() {
            return DanmakuManager.getInstance().getBulletSnapshots(world);
        }

        public BulletSnapshot get(int id) {
            return DanmakuManager.getInstance().getBulletSnapshot(world, id);
        }

        public BulletHandle handle(int id) {
            return new BulletHandle(world, id);
        }
    }

    @PublicApi
    public final class Lasers {
        private Lasers() {}

        public int count() {
            return DanmakuManager.getInstance().getLaserCount(world);
        }

        public boolean contains(int id) {
            return DanmakuManager.getInstance().hasLaser(world, id);
        }

        public List<Integer> ids() {
            return DanmakuManager.getInstance().getLaserIds(world);
        }

        public List<LaserSnapshot> all() {
            return DanmakuManager.getInstance().getLaserSnapshots(world);
        }

        public LaserSnapshot get(int id) {
            return DanmakuManager.getInstance().getLaserSnapshot(world, id);
        }

        public LaserHandle handle(int id) {
            return new LaserHandle(world, id);
        }
    }

    @PublicApi
    public final class Summons {
        private Summons() {}

        public int count() {
            return SummonManager.getInstance().getSummonCount(world);
        }

        public boolean contains(int id) {
            return SummonManager.getInstance().hasSummon(world, id);
        }

        public List<Integer> ids() {
            return SummonManager.getInstance().getSummonIds(world);
        }

        public List<SummonSnapshot> all() {
            return SummonManager.getInstance().getSummonSnapshots(world);
        }

        public SummonSnapshot get(int id) {
            return SummonManager.getInstance().getSummonSnapshot(world, id);
        }

        public List<SummonSnapshot> ownedBy(UUID ownerId) {
            return SummonManager.getInstance().getOwnedSummonSnapshots(world, ownerId);
        }

        public SummonHandle handle(int id) {
            return new SummonHandle(world, id);
        }
    }
}
