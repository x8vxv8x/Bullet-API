package com.smd.bulletapi.common;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.api.runtime.IBulletActor;
import com.smd.bulletapi.api.snapshot.BulletSnapshot;
import com.smd.bulletapi.api.snapshot.LaserSnapshot;
import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.event.BulletCollisionEvent;
import com.smd.bulletapi.event.LaserCollisionEvent;
import com.smd.bulletapi.event.lifecycle.BulletRemoveEvent;
import com.smd.bulletapi.event.lifecycle.BulletSpawnEvent;
import com.smd.bulletapi.event.lifecycle.LaserRemoveEvent;
import com.smd.bulletapi.event.lifecycle.LaserSpawnEvent;
import com.smd.bulletapi.event.lifecycle.LifecycleRemoveReason;
import com.smd.bulletapi.network.PacketHandler;
import com.smd.bulletapi.network.SPacketDanmaku;
import com.smd.bulletapi.network.SPacketLaser;
import com.smd.bulletapi.server.Bullet;
import com.smd.bulletapi.server.Laser;
import com.smd.bulletapi.spi.bullet.IBulletCollisionFilter;
import com.smd.bulletapi.spi.bullet.IBulletHitBehavior;
import com.smd.bulletapi.spi.bullet.IBulletMotionController;
import com.smd.bulletapi.spi.combat.CombatRelation;
import com.smd.bulletapi.spi.combat.CombatRelationResolverRegistry;
import com.smd.bulletapi.spi.laser.ILaserCollisionFilter;
import com.smd.bulletapi.spi.laser.ILaserHitBehavior;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@InternalApi
public class DanmakuManager {
    private static final DanmakuManager INSTANCE = new DanmakuManager();
    private final Map<World, Map<Integer, Bullet>> worldBullets = new HashMap<>();
    private final Map<World, Map<Integer, Laser>> worldLasers = new HashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(0);

    private DanmakuManager() {}

    public static DanmakuManager getInstance() {
        return INSTANCE;
    }

    private Map<Integer, Bullet> getWorldMap(World world) {
        return worldBullets.computeIfAbsent(world, w -> new HashMap<>());
    }

    private Map<Integer, Laser> getLaserWorldMap(World world) {
        return worldLasers.computeIfAbsent(world, w -> new HashMap<>());
    }

    public int spawnBullet(World world, Vec3d position, Vec3d velocity, int life, float damage,
                           String texture, int color, float size, String rendererType,
                           NBTTagCompound customData, ICollisionShape collisionShape,
                           IBulletHitBehavior hitBehavior,
                           IBulletMotionController motionController, Consumer<IBulletActor> tickCallback,
                           IBulletCollisionFilter collisionFilter, boolean onlyPlayer,
                           EntityLivingBase shooter, ItemStack shooterHeldItem,
                           AttackSourceInfo attackSourceInfo) {
        if (world.isRemote) return -1;
        int id = nextId.getAndIncrement();
        Bullet bullet = new Bullet(id, position, velocity, life, damage,
                texture, color, size, rendererType, customData,
                collisionShape, hitBehavior, motionController, tickCallback, collisionFilter, onlyPlayer,
                shooter, shooterHeldItem, attackSourceInfo);
        getWorldMap(world).put(id, bullet);
        MinecraftForge.EVENT_BUS.post(new BulletSpawnEvent(world, createBulletSnapshot(bullet)));
        PacketHandler.sendToDimension(SPacketDanmaku.createSpawn(
                id, position, velocity, life, damage, texture, color, size, rendererType, customData),
                world.provider.getDimension());
        return id;
    }

    public void removeBullet(World world, int id) {
        removeBullet(world, id, LifecycleRemoveReason.API_REQUEST);
    }

    public void removeBullet(World world, int id, LifecycleRemoveReason reason) {
        Map<Integer, Bullet> map = worldBullets.get(world);
        if (map == null) return;

        Bullet removed = map.remove(id);
        if (removed == null) return;

        MinecraftForge.EVENT_BUS.post(new BulletRemoveEvent(world, createBulletSnapshot(removed), reason));
        PacketHandler.sendToDimension(SPacketDanmaku.createRemove(id), world.provider.getDimension());
    }

    public void updateBulletVelocity(World world, int id, Vec3d newVelocity) {
        Bullet bullet = getLiveBullet(world, id);
        if (bullet == null) return;
        bullet.setVelocity(newVelocity);
        syncBullet(world, bullet, SPacketDanmaku.FLAG_VELOCITY);
    }

    public void updateBulletPosition(World world, int id, Vec3d newPosition) {
        Bullet bullet = getLiveBullet(world, id);
        if (bullet == null) return;
        bullet.setPosition(newPosition);
        syncBullet(world, bullet, SPacketDanmaku.FLAG_POSITION);
    }

    public void updateBulletMotion(World world, int id, Vec3d newPosition, Vec3d newVelocity) {
        Bullet bullet = getLiveBullet(world, id);
        if (bullet == null) return;
        bullet.setPosition(newPosition);
        bullet.setVelocity(newVelocity);
        syncBullet(world, bullet, SPacketDanmaku.FLAG_POSITION | SPacketDanmaku.FLAG_VELOCITY);
    }

    public void updateBulletLife(World world, int id, int life) {
        Bullet bullet = getLiveBullet(world, id);
        if (bullet == null) return;
        bullet.setLife(life);
        syncBullet(world, bullet, SPacketDanmaku.FLAG_LIFE);
    }

    public boolean hasBullet(World world, int id) {
        Map<Integer, Bullet> map = worldBullets.get(world);
        if (map == null) return false;
        Bullet bullet = map.get(id);
        return bullet != null && !bullet.isDead();
    }

    public BulletSnapshot getBulletSnapshot(World world, int id) {
        Bullet bullet = getLiveBullet(world, id);
        return bullet == null ? null : createBulletSnapshot(bullet);
    }

    public int spawnLaser(World world, Vec3d start, Vec3d direction, double maxLength,
                          float thickness, int life, float damage, int color,
                          String rendererType, NBTTagCompound customData,
                          boolean penetrate, boolean followShooter,
                          boolean onlyPlayer, boolean blockStops,
                          Vec3d startOffset,
                          Vec3d startOffsetLocal,
                          int eventIntervalTicks,
                          ILaserHitBehavior hitBehavior,
                          ILaserCollisionFilter collisionFilter,
                          EntityLivingBase shooter, ItemStack shooterHeldItem,
                          AttackSourceInfo attackSourceInfo) {
        if (world.isRemote) return -1;
        Vec3d offset = startOffset == null ? new Vec3d(0, 0, 0) : startOffset;
        Vec3d offsetLocal = startOffsetLocal == null ? new Vec3d(0, 0, 0) : startOffsetLocal;
        if ((start == null || direction == null) && followShooter && shooter != null) {
            if (start == null) {
                Vec3d look = shooter.getLookVec();
                Vec3d local = toLocalOffset(look, offsetLocal);
                start = shooter.getPositionEyes(1.0f).add(offset).add(local);
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
                life, penetrate, followShooter, onlyPlayer, blockStops, offset, offsetLocal,
                eventIntervalTicks, hitBehavior, color, rendererType, customData, collisionFilter,
                shooter, shooterHeldItem, attackSourceInfo);
        laser.setCurrentLength(computeLaserLength(laser, world));
        getLaserWorldMap(world).put(id, laser);
        MinecraftForge.EVENT_BUS.post(new LaserSpawnEvent(world, createLaserSnapshot(laser)));
        PacketHandler.sendToDimension(SPacketLaser.createSpawn(
                id, world.getTotalWorldTime(), laser.getStart(), laser.getDirection(), laser.getCurrentLength(),
                laser.getThickness(), laser.getColor(), laser.getRendererType(), laser.getCustomData()),
                world.provider.getDimension());
        return id;
    }

    public void removeLaser(World world, int id) {
        removeLaser(world, id, LifecycleRemoveReason.API_REQUEST);
    }

    public void removeLaser(World world, int id, LifecycleRemoveReason reason) {
        Map<Integer, Laser> map = worldLasers.get(world);
        if (map == null) return;

        Laser removed = map.remove(id);
        if (removed == null) return;

        MinecraftForge.EVENT_BUS.post(new LaserRemoveEvent(world, createLaserSnapshot(removed), reason));
        PacketHandler.sendToDimension(SPacketLaser.createRemove(id), world.provider.getDimension());
    }

    public boolean hasLaser(World world, int id) {
        Map<Integer, Laser> map = worldLasers.get(world);
        if (map == null) return false;
        Laser laser = map.get(id);
        return laser != null && !laser.isDead();
    }

    public LaserSnapshot getLaserSnapshot(World world, int id) {
        Laser laser = getLiveLaser(world, id);
        return laser == null ? null : createLaserSnapshot(laser);
    }

    public boolean updateLaserTransform(World world, int id, Vec3d start, Vec3d direction) {
        Laser laser = getLiveLaser(world, id);
        if (laser == null || laser.isDead()) return false;

        laser.setStart(start);
        laser.setDirection(direction);
        laser.setCurrentLength(computeLaserLength(laser, world));
        syncLaser(world, laser, SPacketLaser.FLAG_START | SPacketLaser.FLAG_DIRECTION | SPacketLaser.FLAG_LENGTH);
        return true;
    }

    public boolean updateLaserLength(World world, int id, double length) {
        Laser laser = getLiveLaser(world, id);
        if (laser == null) return false;
        laser.setCurrentLength(Math.max(0.0D, Math.min(length, laser.getMaxLength())));
        syncLaser(world, laser, SPacketLaser.FLAG_LENGTH);
        return true;
    }

    public void updateLaserLife(World world, int id, int life) {
        Laser laser = getLiveLaser(world, id);
        if (laser == null) return;
        laser.setLife(life);
        if (life == 0) {
            removeLaser(world, id, LifecycleRemoveReason.API_REQUEST);
            return;
        }
        syncLaser(world, laser, 0);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        World world = event.world;
        if (world.isRemote || event.phase != TickEvent.Phase.END) return;

        Map<Integer, Laser> laserMap = worldLasers.get(world);
        if (laserMap != null) {
            Map<Integer, Vec3d> previousLaserStarts = new HashMap<>();
            Map<Integer, Vec3d> previousLaserDirections = new HashMap<>();
            Map<Integer, Double> previousLaserLengths = new HashMap<>();
            for (Laser laser : laserMap.values()) {
                previousLaserStarts.put(laser.getId(), laser.getStart());
                previousLaserDirections.put(laser.getId(), laser.getDirection());
                previousLaserLengths.put(laser.getId(), laser.getCurrentLength());
                laser.update(world);
            }

            List<Laser> lasers = new ArrayList<>(laserMap.values());
            for (Laser laser : lasers) {
                if (laser.isDead()) continue;
                if (laserMap.get(laser.getId()) != laser) continue;

                Vec3d previousStart = previousLaserStarts.get(laser.getId());
                Vec3d previousDirection = previousLaserDirections.get(laser.getId());
                double previousLength = previousLaserLengths.getOrDefault(laser.getId(), laser.getCurrentLength());
                double length = computeLaserLength(laser, world);
                laser.setCurrentLength(length);
                handleLaserCollision(laser, world);
                int laserFlags = 0;
                if (!sameVec(previousStart, laser.getStart())) laserFlags |= SPacketLaser.FLAG_START;
                if (!sameVec(previousDirection, laser.getDirection())) laserFlags |= SPacketLaser.FLAG_DIRECTION;
                if (!sameDouble(previousLength, laser.getCurrentLength())) laserFlags |= SPacketLaser.FLAG_LENGTH;
                syncLaser(world, laser, laserFlags);
            }

            removeDeadLasers(world, laserMap, LifecycleRemoveReason.EXPIRED);
        }

        Map<Integer, Bullet> map = worldBullets.get(world);
        if (map != null) {
            for (Bullet bullet : map.values()) {
                bullet.update(world);
            }

            List<Bullet> bullets = new ArrayList<>(map.values());
            List<EntityLivingBase> fallbackEntities = null;

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
                        if (!canBulletCollide(world, bullet, player)) continue;
                        if (shape.checkCollision(posX, posY, posZ, player)) {
                            handleCollision(bullet, world, player);
                            if (bullet.isDead() || map.get(bullet.getId()) != bullet) {
                                break;
                            }
                        }
                    }
                } else {
                    if (fallbackEntities == null && shape.getBroadphaseRadius() <= 0.0D) {
                        fallbackEntities = buildFallbackEntities(world);
                    }
                    List<EntityLivingBase> candidates = getCollisionCandidates(world, bullet, shape, fallbackEntities);
                    for (EntityLivingBase entity : candidates) {
                        if (entity.isDead) continue;
                        if (!canBulletCollide(world, bullet, entity)) continue;
                        if (shape.checkCollision(posX, posY, posZ, entity)) {
                            handleCollision(bullet, world, entity);
                            if (bullet.isDead() || map.get(bullet.getId()) != bullet) {
                                break;
                            }
                        }
                    }
                }
            }

            removeDeadBullets(world, map, LifecycleRemoveReason.EXPIRED);
        }
    }

    private List<EntityLivingBase> getCollisionCandidates(World world, Bullet bullet, ICollisionShape shape,
                                                          List<EntityLivingBase> fallbackEntities) {
        double radius = shape.getBroadphaseRadius();
        if (radius <= 0.0D) {
            return fallbackEntities == null ? buildFallbackEntities(world) : fallbackEntities;
        }

        double x = bullet.getPosX();
        double y = bullet.getPosY();
        double z = bullet.getPosZ();
        AxisAlignedBB searchBox = new AxisAlignedBB(x, y, z, x, y, z).grow(radius + 4.0D);
        return world.getEntitiesWithinAABB(EntityLivingBase.class, searchBox);
    }

    private List<EntityLivingBase> buildFallbackEntities(World world) {
        List<EntityLivingBase> fallbackEntities = new ArrayList<>();
        for (Entity entity : world.loadedEntityList) {
            if (!entity.isDead && entity instanceof EntityLivingBase) {
                fallbackEntities.add((EntityLivingBase) entity);
            }
        }
        return fallbackEntities;
    }

    private void handleCollision(Bullet bullet, World world, Entity entity) {
        CollisionContext ctx = new CollisionContext(bullet, world, entity);
        BulletCollisionEvent eventBus = new BulletCollisionEvent(world, bullet, entity, ctx);
        MinecraftForge.EVENT_BUS.post(eventBus);
        if (!eventBus.isCanceled()) {
            bullet.handleHit(ctx);
            if (!ctx.canceled) {
                entity.attackEntityFrom(DamageSource.GENERIC, ctx.damage);
            }
        }
    }

    private boolean canBulletCollide(World world, Bullet bullet, EntityLivingBase entity) {
        if (entity == null || entity.isDead) return false;
        if (bullet.getShooter() == entity) return false;

        AttackSourceInfo source = bullet.getAttackSourceInfo();
        if (source != null && source.getOwnerId() != null && source.getOwnerId().equals(entity.getUniqueID())) {
            return false;
        }

        if (entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.disableDamage) {
            return false;
        }

        EntityLivingBase shooter = bullet.getShooter();
        boolean defaultAllowed = shooter == null || shooter == entity || !shooter.isOnSameTeam(entity);

        IBulletCollisionFilter filter = bullet.getCollisionFilter();
        if (filter != null && !filter.canCollide(world, bullet, entity)) {
            return false;
        }

        CombatRelation relation = CombatRelationResolverRegistry.resolveBullet(world, bullet, entity);
        if (relation == CombatRelation.DENY) {
            return false;
        }
        return relation == CombatRelation.ALLOW || defaultAllowed;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        World world = event.getWorld();
        Map<Integer, Bullet> removedBullets = worldBullets.remove(world);
        if (removedBullets != null) {
            for (Bullet bullet : removedBullets.values()) {
                MinecraftForge.EVENT_BUS.post(new BulletRemoveEvent(world, createBulletSnapshot(bullet), LifecycleRemoveReason.WORLD_UNLOAD));
            }
        }

        Map<Integer, Laser> removedLasers = worldLasers.remove(world);
        if (removedLasers != null) {
            for (Laser laser : removedLasers.values()) {
                MinecraftForge.EVENT_BUS.post(new LaserRemoveEvent(world, createLaserSnapshot(laser), LifecycleRemoveReason.WORLD_UNLOAD));
            }
        }
    }

    public int getBulletCount(World world) {
        Map<Integer, Bullet> map = worldBullets.get(world);
        if (map == null) return 0;
        int count = 0;
        for (Bullet bullet : map.values()) {
            if (!bullet.isDead()) count++;
        }
        return count;
    }

    public int getLaserCount(World world) {
        Map<Integer, Laser> map = worldLasers.get(world);
        if (map == null) return 0;
        int count = 0;
        for (Laser laser : map.values()) {
            if (!laser.isDead()) count++;
        }
        return count;
    }

    public List<Integer> getBulletIds(World world) {
        Map<Integer, Bullet> map = worldBullets.get(world);
        if (map == null || map.isEmpty()) return java.util.Collections.emptyList();
        List<Integer> ids = new ArrayList<>();
        for (Map.Entry<Integer, Bullet> entry : map.entrySet()) {
            if (!entry.getValue().isDead()) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }

    public List<Integer> getLaserIds(World world) {
        Map<Integer, Laser> map = worldLasers.get(world);
        if (map == null || map.isEmpty()) return java.util.Collections.emptyList();
        List<Integer> ids = new ArrayList<>();
        for (Map.Entry<Integer, Laser> entry : map.entrySet()) {
            if (!entry.getValue().isDead()) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }

    public List<BulletSnapshot> getBulletSnapshots(World world) {
        Map<Integer, Bullet> map = worldBullets.get(world);
        if (map == null || map.isEmpty()) return java.util.Collections.emptyList();
        List<BulletSnapshot> snapshots = new ArrayList<>();
        for (Bullet bullet : map.values()) {
            if (!bullet.isDead()) {
                snapshots.add(createBulletSnapshot(bullet));
            }
        }
        return snapshots;
    }

    public List<LaserSnapshot> getLaserSnapshots(World world) {
        Map<Integer, Laser> map = worldLasers.get(world);
        if (map == null || map.isEmpty()) return java.util.Collections.emptyList();
        List<LaserSnapshot> snapshots = new ArrayList<>();
        for (Laser laser : map.values()) {
            if (!laser.isDead()) {
                snapshots.add(createLaserSnapshot(laser));
            }
        }
        return snapshots;
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
        if (length <= 0.0D) return;
        Vec3d end = start.add(dir.scale(length));

        AxisAlignedBB searchBox = buildSegmentAabb(start, end, laser.getThickness());
        List<EntityLivingBase> candidates = world.getEntitiesWithinAABB(EntityLivingBase.class, searchBox);

        long worldTick = world.getTotalWorldTime();
        if (!laser.isPenetrate()) {
            EntityLivingBase closest = null;
            double closestT = Double.MAX_VALUE;
            for (EntityLivingBase entity : candidates) {
                if (entity.isDead) continue;
                if (!canLaserCollide(world, laser, entity)) continue;
                double tEntry = segmentBoxEntry(start, end, entity.getEntityBoundingBox().grow(laser.getThickness()));
                if (!Double.isNaN(tEntry) && tEntry < closestT) {
                    closestT = tEntry;
                    closest = entity;
                }
            }

            if (closest != null) {
                double clampedT = Math.max(0.0D, Math.min(1.0D, closestT));
                laser.setCurrentLength(length * clampedT);
                if (laser.canTrigger(closest, worldTick)) {
                    handleLaserHit(laser, world, closest, worldTick);
                }
            }
            return;
        }

        for (EntityLivingBase entity : candidates) {
            if (entity.isDead) continue;
            if (!canLaserCollide(world, laser, entity)) continue;
            double tEntry = segmentBoxEntry(start, end, entity.getEntityBoundingBox().grow(laser.getThickness()));
            if (!Double.isNaN(tEntry) && laser.canTrigger(entity, worldTick)) {
                handleLaserHit(laser, world, entity, worldTick);
            }
        }
    }

    private void handleLaserHit(Laser laser, World world, EntityLivingBase entity, long worldTick) {
        LaserCollisionContext ctx = new LaserCollisionContext(laser, world, entity);
        LaserCollisionEvent eventBus = new LaserCollisionEvent(world, laser, entity, ctx);
        MinecraftForge.EVENT_BUS.post(eventBus);
        if (!eventBus.isCanceled()) {
            laser.handleHit(ctx);
            if (!ctx.canceled) {
                entity.attackEntityFrom(DamageSource.GENERIC, ctx.damage);
            }
        }
        laser.markTriggered(entity, worldTick);
    }

    private boolean canLaserCollide(World world, Laser laser, EntityLivingBase entity) {
        if (laser.getShooter() == entity) return false;
        AttackSourceInfo source = laser.getAttackSourceInfo();
        if (source != null && source.getOwnerId() != null && source.getOwnerId().equals(entity.getUniqueID())) {
            return false;
        }
        if (laser.isOnlyPlayer() && !(entity instanceof EntityPlayer)) return false;
        if (entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.disableDamage) {
            return false;
        }

        EntityLivingBase shooter = laser.getShooter();
        boolean defaultAllowed = shooter == null || !shooter.isOnSameTeam(entity);

        ILaserCollisionFilter filter = laser.getCollisionFilter();
        if (filter != null && !filter.canCollide(world, laser, entity)) {
            return false;
        }

        CombatRelation relation = CombatRelationResolverRegistry.resolveLaser(world, laser, entity);
        if (relation == CombatRelation.DENY) {
            return false;
        }
        return relation == CombatRelation.ALLOW || defaultAllowed;
    }

    private void removeDeadBullets(World world, Map<Integer, Bullet> map, LifecycleRemoveReason reason) {
        List<Integer> deadIds = new ArrayList<>();
        for (Map.Entry<Integer, Bullet> entry : map.entrySet()) {
            if (entry.getValue().isDead()) {
                deadIds.add(entry.getKey());
            }
        }
        for (Integer deadId : deadIds) {
            removeBullet(world, deadId, reason);
        }
    }

    private void removeDeadLasers(World world, Map<Integer, Laser> map, LifecycleRemoveReason reason) {
        List<Integer> deadIds = new ArrayList<>();
        for (Map.Entry<Integer, Laser> entry : map.entrySet()) {
            if (entry.getValue().isDead()) {
                deadIds.add(entry.getKey());
            }
        }
        for (Integer deadId : deadIds) {
            removeLaser(world, deadId, reason);
        }
    }

    private BulletSnapshot createBulletSnapshot(Bullet bullet) {
        return new BulletSnapshot(
                bullet.getId(),
                bullet.getPosition(),
                bullet.getVelocity(),
                bullet.getLife(),
                bullet.getDamage(),
                bullet.isOnlyPlayer(),
                bullet.getAttackSourceInfo()
        );
    }

    private LaserSnapshot createLaserSnapshot(Laser laser) {
        return new LaserSnapshot(
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
    }

    private Bullet getLiveBullet(World world, int id) {
        Map<Integer, Bullet> map = worldBullets.get(world);
        if (map == null) return null;
        Bullet bullet = map.get(id);
        return bullet == null || bullet.isDead() ? null : bullet;
    }

    private Laser getLiveLaser(World world, int id) {
        Map<Integer, Laser> map = worldLasers.get(world);
        if (map == null) return null;
        Laser laser = map.get(id);
        return laser == null || laser.isDead() ? null : laser;
    }

    private void syncBullet(World world, Bullet bullet, int flags) {
        if (bullet == null) return;
        if (bullet.isDead() || bullet.getLife() <= 0) {
            removeBullet(world, bullet.getId(), LifecycleRemoveReason.API_REQUEST);
            return;
        }
        if (flags == 0) return;
        PacketHandler.sendToDimension(
                SPacketDanmaku.createUpdate(
                        bullet.getId(),
                        flags,
                        (flags & SPacketDanmaku.FLAG_POSITION) != 0 ? bullet.getPosition() : null,
                        (flags & SPacketDanmaku.FLAG_VELOCITY) != 0 ? bullet.getVelocity() : null,
                        (flags & SPacketDanmaku.FLAG_LIFE) != 0 ? bullet.getLife() : null
                ),
                world.provider.getDimension()
        );
    }

    private void syncLaser(World world, Laser laser, int flags) {
        if (laser == null) return;
        if (laser.isDead() || laser.getLife() == 0) {
            removeLaser(world, laser.getId(), LifecycleRemoveReason.API_REQUEST);
            return;
        }
        if (flags == 0) return;
        PacketHandler.sendToDimension(
                SPacketLaser.createUpdate(
                        laser.getId(),
                        world.getTotalWorldTime(),
                        flags,
                        (flags & SPacketLaser.FLAG_START) != 0 ? laser.getStart() : null,
                        (flags & SPacketLaser.FLAG_DIRECTION) != 0 ? laser.getDirection() : null,
                        (flags & SPacketLaser.FLAG_LENGTH) != 0 ? laser.getCurrentLength() : null
                ),
                world.provider.getDimension()
        );
    }

    private static boolean sameVec(Vec3d a, Vec3d b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.squareDistanceTo(b) < 1.0E-8;
    }

    private static boolean sameDouble(double a, double b) {
        return Math.abs(a - b) < 1.0E-8;
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

    private static Vec3d toLocalOffset(Vec3d look, Vec3d local) {
        if (local == null) return new Vec3d(0, 0, 0);
        if (look == null || look.lengthSquared() < 1.0E-6) return new Vec3d(0, 0, 0);
        Vec3d forward = look.normalize();
        Vec3d upRef = new Vec3d(0, 1, 0);
        Vec3d right = forward.crossProduct(upRef);
        if (right.lengthSquared() < 1.0E-6) {
            upRef = new Vec3d(1, 0, 0);
            right = forward.crossProduct(upRef);
        }
        right = right.normalize();
        Vec3d up = right.crossProduct(forward).normalize();
        return right.scale(local.x).add(up.scale(local.y)).add(forward.scale(local.z));
    }

    private double segmentBoxEntry(Vec3d start, Vec3d end, AxisAlignedBB box) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;

        double tmin = 0.0D;
        double tmax = 1.0D;

        if (Math.abs(dx) < 1.0E-8) {
            if (start.x < box.minX || start.x > box.maxX) return Double.NaN;
        } else {
            double inv = 1.0D / dx;
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
            double inv = 1.0D / dy;
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
            double inv = 1.0D / dz;
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
