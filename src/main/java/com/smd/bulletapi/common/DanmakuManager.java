package com.smd.bulletapi.common;

import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.event.BulletCollisionEvent;
import com.smd.bulletapi.event.LaserCollisionEvent;
import com.smd.bulletapi.network.PacketHandler;
import com.smd.bulletapi.network.SPacketDanmaku;
import com.smd.bulletapi.network.SPacketLaser;
import com.smd.bulletapi.server.Bullet;
import com.smd.bulletapi.server.Laser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DanmakuManager {
    private static final DanmakuManager INSTANCE = new DanmakuManager();
    private final Map<World, Map<Integer, Bullet>> worldBullets = new ConcurrentHashMap<>();
    private final Map<World, Map<Integer, Laser>> worldLasers = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(0);

    private DanmakuManager() {}

    public static DanmakuManager getInstance() {
        return INSTANCE;
    }

    private Map<Integer, Bullet> getWorldMap(World world) {
        return worldBullets.computeIfAbsent(world, w -> new ConcurrentHashMap<>());
    }

    private Map<Integer, Laser> getLaserWorldMap(World world) {
        return worldLasers.computeIfAbsent(world, w -> new ConcurrentHashMap<>());
    }

    public int spawnBullet(World world, Vec3d position, Vec3d velocity, int life, float damage,
                           String texture, int color, float size, String rendererType,
                           NBTTagCompound customData, ICollisionShape collisionShape,
                           Consumer<CollisionContext> onCollision, Consumer<Bullet> tickCallback,
                           boolean onlyPlayer, EntityLivingBase shooter, ItemStack shooterHeldItem) {
        if (world.isRemote) return -1;
        int id = nextId.getAndIncrement();
        Bullet bullet = new Bullet(id, position, velocity, life, damage,
                texture, color, size, rendererType, customData,
                collisionShape, onCollision, tickCallback, onlyPlayer,
                shooter, shooterHeldItem);
        getWorldMap(world).put(id, bullet);
        PacketHandler.sendToDimension(SPacketDanmaku.createSpawn(
                id, position, velocity, life, damage, texture, color, size, rendererType, customData),
                world.provider.getDimension());
        return id;
    }

    public void removeBullet(World world, int id) {
        Map<Integer, Bullet> map = worldBullets.get(world);
        if (map != null) {
            map.remove(id);
            PacketHandler.sendToDimension(SPacketDanmaku.createRemove(id), world.provider.getDimension());
        }
    }

    public void updateBulletVelocity(World world, int id, Vec3d newVelocity) {
        Map<Integer, Bullet> map = worldBullets.get(world);
        if (map != null) {
            Bullet bullet = map.get(id);
            if (bullet != null) {
                bullet.setVelocity(newVelocity);
                PacketHandler.sendToDimension(SPacketDanmaku.createUpdate(id, newVelocity), world.provider.getDimension());
            }
        }
    }

    public int spawnLaser(World world, Vec3d start, Vec3d direction, double maxLength,
                          float thickness, int life, float damage, int color,
                          String rendererType, NBTTagCompound customData,
                          boolean penetrate, boolean followShooter,
                          boolean onlyPlayer, boolean blockStops,
                          Vec3d startOffset,
                          int eventIntervalTicks,
                          Consumer<LaserCollisionContext> onCollision,
                          EntityLivingBase shooter, ItemStack shooterHeldItem) {
        if (world.isRemote) return -1;
        Vec3d offset = startOffset == null ? new Vec3d(0, 0, 0) : startOffset;
        if ((start == null || direction == null) && followShooter && shooter != null) {
            if (start == null) {
                start = shooter.getPositionEyes(1.0f).add(offset);
            }
            if (direction == null) {
                direction = shooter.getLookVec();
            }
        }
        if (start == null) {
            start = new Vec3d(0, 0, 0);
        }
        if (direction == null || direction.lengthSquared() < 1.0E-6) {
            direction = new Vec3d(0, 0, 1);
        }
        int id = nextId.getAndIncrement();
        Laser laser = new Laser(id, start, direction, maxLength, thickness, damage,
                life, penetrate, followShooter, onlyPlayer, blockStops, offset,
                eventIntervalTicks,
                color, rendererType, customData, onCollision, shooter, shooterHeldItem);
        getLaserWorldMap(world).put(id, laser);
        PacketHandler.sendToDimension(SPacketLaser.createSpawn(
                id, world.getTotalWorldTime(), laser.getStart(), laser.getDirection(), laser.getCurrentLength(),
                laser.getThickness(), laser.getColor(), laser.getRendererType(), laser.getCustomData()),
                world.provider.getDimension());
        return id;
    }

    public void removeLaser(World world, int id) {
        Map<Integer, Laser> map = worldLasers.get(world);
        if (map != null) {
            map.remove(id);
            PacketHandler.sendToDimension(SPacketLaser.createRemove(id), world.provider.getDimension());
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        World world = event.world;
        if (world.isRemote) return;
        if (event.phase == TickEvent.Phase.END) {
            Map<Integer, Laser> laserMap = worldLasers.get(world);
            if (laserMap != null) {
                for (Laser laser : laserMap.values()) {
                    laser.update(world);
                }

                List<Laser> lasers = new ArrayList<>(laserMap.values());
                for (Laser laser : lasers) {
                    if (laser.isDead()) continue;
                    if (laserMap.get(laser.getId()) != laser) continue;

                    double length = computeLaserLength(laser, world);
                    laser.setCurrentLength(length);

                    handleLaserCollision(laser, world);

                    PacketHandler.sendToDimension(SPacketLaser.createUpdate(
                            laser.getId(), world.getTotalWorldTime(), laser.getStart(), laser.getDirection(), laser.getCurrentLength()),
                            world.provider.getDimension());
                }

                for (Map.Entry<Integer, Laser> entry : laserMap.entrySet()) {
                    if (entry.getValue().isDead()) {
                        PacketHandler.sendToDimension(SPacketLaser.createRemove(entry.getKey()),
                                world.provider.getDimension());
                    }
                }
                laserMap.entrySet().removeIf(entry -> entry.getValue().isDead());
            }

            Map<Integer, Bullet> map = worldBullets.get(world);
            if (map != null) {
                // update bullet positions first
                for (Bullet bullet : map.values()) {
                    bullet.update(world);
                }

                List<Bullet> bullets = new ArrayList<>(map.values());
                List<EntityLivingBase> fallbackEntities = new ArrayList<>();
                for (Entity entity : world.loadedEntityList) {
                    if (!entity.isDead && entity instanceof EntityLivingBase) {
                        fallbackEntities.add((EntityLivingBase) entity);
                    }
                }

                for (Bullet bullet : bullets) {
                    ICollisionShape shape = bullet.getCollisionShape();
                    if (shape == null || bullet.isDead()) continue;
                    if (map.get(bullet.getId()) != bullet) continue;

                    double posX = bullet.getPosX();
                    double posY = bullet.getPosY();
                    double posZ = bullet.getPosZ();
                    if (bullet.isOnlyPlayer()) {
                        for (EntityPlayer player : world.playerEntities) {
                            if (player.isDead || player.capabilities.disableDamage) continue;
                            if (shape.checkCollision(posX, posY, posZ, player)) {
                                handleCollision(bullet, world, player);
                                if (bullet.isDead() || map.get(bullet.getId()) != bullet) {
                                    break;
                                }
                            }
                        }
                    } else {
                        List<EntityLivingBase> candidates = getCollisionCandidates(world, bullet, shape, fallbackEntities);
                        for (EntityLivingBase entity : candidates) {
                            if (entity.isDead) continue;
                            if (shape.checkCollision(posX, posY, posZ, entity)) {
                                handleCollision(bullet, world, entity);
                                if (bullet.isDead() || map.get(bullet.getId()) != bullet) {
                                    break;
                                }
                            }
                        }
                    }
                }

                map.entrySet().removeIf(entry -> entry.getValue().isDead());
            }
        }
    }

    private List<EntityLivingBase> getCollisionCandidates(World world, Bullet bullet, ICollisionShape shape,
                                                          List<EntityLivingBase> fallbackEntities) {
        double radius = shape.getBroadphaseRadius();
        if (radius <= 0.0D) {
            return fallbackEntities;
        }

        double x = bullet.getPosX();
        double y = bullet.getPosY();
        double z = bullet.getPosZ();
        AxisAlignedBB searchBox = new AxisAlignedBB(x, y, z, x, y, z).grow(radius + 4.0D);
        return world.getEntitiesWithinAABB(EntityLivingBase.class, searchBox);
    }

    private void handleCollision(Bullet bullet, World world, Entity entity) {
        // 单次碰撞链路（事件 -> 回调 -> 默认伤害）复用同一个 CollisionContext。
        CollisionContext ctx = new CollisionContext(bullet, world, entity);
        BulletCollisionEvent eventBus = new BulletCollisionEvent(world, bullet, entity, ctx);
        MinecraftForge.EVENT_BUS.post(eventBus);
        if (!eventBus.isCanceled()) {
            bullet.onCollision(ctx);
            if (!ctx.canceled) {
                entity.attackEntityFrom(DamageSource.GENERIC, ctx.damage);
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        worldBullets.remove(event.getWorld());
        worldLasers.remove(event.getWorld());
    }

    public int getBulletCount(World world) {
        Map<Integer, Bullet> map = worldBullets.get(world);
        return map == null ? 0 : map.size();
    }

    private double computeLaserLength(Laser laser, World world) {
        double maxLength = laser.getMaxLength();
        if (!laser.isBlockStops()) return maxLength;
        Vec3d start = laser.getStart();
        Vec3d end = start.add(laser.getDirection().scale(maxLength));
        RayTraceResult hit = world.rayTraceBlocks(start, end, false, true, false);
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK && hit.hitVec != null) {
            return start.distanceTo(hit.hitVec);
        }
        return maxLength;
    }

    private void handleLaserCollision(Laser laser, World world) {
        Vec3d start = laser.getStart();
        Vec3d dir = laser.getDirection();
        double length = laser.getCurrentLength();
        if (length <= 0.0) return;
        Vec3d end = start.add(dir.scale(length));

        AxisAlignedBB searchBox = buildSegmentAabb(start, end, laser.getThickness());
        List<EntityLivingBase> candidates = world.getEntitiesWithinAABB(EntityLivingBase.class, searchBox);

        long worldTick = world.getTotalWorldTime();
        if (!laser.isPenetrate()) {
            EntityLivingBase closest = null;
            double closestT = Double.MAX_VALUE;
            for (EntityLivingBase entity : candidates) {
                if (entity.isDead) continue;
                if (!canLaserCollide(laser, entity)) continue;
                double tEntry = segmentBoxEntry(start, end, entity.getEntityBoundingBox().grow(laser.getThickness()));
                if (!Double.isNaN(tEntry) && tEntry < closestT) {
                    closestT = tEntry;
                    closest = entity;
                }
            }

            if (closest != null) {
                double clampedT = Math.max(0.0, Math.min(1.0, closestT));
                laser.setCurrentLength(length * clampedT);
                if (laser.canTrigger(closest, worldTick)) {
                    handleLaserHit(laser, world, closest, worldTick);
                }
            }
            return;
        }

        for (EntityLivingBase entity : candidates) {
            if (entity.isDead) continue;
            if (!canLaserCollide(laser, entity)) continue;
            double tEntry = segmentBoxEntry(start, end, entity.getEntityBoundingBox().grow(laser.getThickness()));
            if (!Double.isNaN(tEntry)) {
                if (laser.canTrigger(entity, worldTick)) {
                    handleLaserHit(laser, world, entity, worldTick);
                }
            }
        }
    }

    private void handleLaserHit(Laser laser, World world, EntityLivingBase entity, long worldTick) {
        LaserCollisionContext ctx = new LaserCollisionContext(laser, world, entity);
        LaserCollisionEvent eventBus = new LaserCollisionEvent(world, laser, entity, ctx);
        MinecraftForge.EVENT_BUS.post(eventBus);
        if (!eventBus.isCanceled()) {
            laser.onCollision(ctx);
            if (!ctx.canceled) {
                entity.attackEntityFrom(DamageSource.GENERIC, ctx.damage);
            }
        }
        laser.markTriggered(entity, worldTick);
    }

    private boolean canLaserCollide(Laser laser, EntityLivingBase entity) {
        if (laser.getShooter() == entity) return false;
        if (laser.isOnlyPlayer()) {
            if (!(entity instanceof EntityPlayer)) return false;
        }
        if (entity instanceof EntityPlayer) {
            return !((EntityPlayer) entity).capabilities.disableDamage;
        }
        return true;
    }

    private AxisAlignedBB buildSegmentAabb(Vec3d start, Vec3d end, float thickness) {
        double minX = Math.min(start.x, end.x);
        double minY = Math.min(start.y, end.y);
        double minZ = Math.min(start.z, end.z);
        double maxX = Math.max(start.x, end.x);
        double maxY = Math.max(start.y, end.y);
        double maxZ = Math.max(start.z, end.z);
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ).grow(thickness);
    }

    private double segmentBoxEntry(Vec3d start, Vec3d end, AxisAlignedBB box) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;

        double tmin = 0.0;
        double tmax = 1.0;

        if (Math.abs(dx) < 1.0E-8) {
            if (start.x < box.minX || start.x > box.maxX) return Double.NaN;
        } else {
            double inv = 1.0 / dx;
            double t1 = (box.minX - start.x) * inv;
            double t2 = (box.maxX - start.x) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return Double.NaN;
        }

        if (Math.abs(dy) < 1.0E-8) {
            if (start.y < box.minY || start.y > box.maxY) return Double.NaN;
        } else {
            double inv = 1.0 / dy;
            double t1 = (box.minY - start.y) * inv;
            double t2 = (box.maxY - start.y) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return Double.NaN;
        }

        if (Math.abs(dz) < 1.0E-8) {
            if (start.z < box.minZ || start.z > box.maxZ) return Double.NaN;
        } else {
            double inv = 1.0 / dz;
            double t1 = (box.minZ - start.z) * inv;
            double t2 = (box.maxZ - start.z) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return Double.NaN;
        }

        return tmin;
    }
}
