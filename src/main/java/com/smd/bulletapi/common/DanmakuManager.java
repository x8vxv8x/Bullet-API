package com.smd.bulletapi.common;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.api.runtime.IBulletActor;
import com.smd.bulletapi.api.snapshot.BulletSnapshot;
import com.smd.bulletapi.api.snapshot.LaserSnapshot;
import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.common.runtime.WorldRuntimeStore;
import com.smd.bulletapi.common.runtime.danmaku.DanmakuSnapshotFactory;
import com.smd.bulletapi.common.runtime.danmaku.DanmakuSyncService;
import com.smd.bulletapi.event.BulletCollisionEvent;
import com.smd.bulletapi.event.LaserCollisionEvent;
import com.smd.bulletapi.event.lifecycle.BulletRemoveEvent;
import com.smd.bulletapi.event.lifecycle.BulletSpawnEvent;
import com.smd.bulletapi.event.lifecycle.LaserRemoveEvent;
import com.smd.bulletapi.event.lifecycle.LaserSpawnEvent;
import com.smd.bulletapi.event.lifecycle.LifecycleRemoveReason;
import com.smd.bulletapi.network.SPacketBulletVisual;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@InternalApi
public class DanmakuManager {
    private static final DanmakuManager INSTANCE = new DanmakuManager();
    private final WorldRuntimeStore<Bullet> bulletStore = new WorldRuntimeStore<>();
    private final WorldRuntimeStore<Laser> laserStore = new WorldRuntimeStore<>();
    private final DanmakuSnapshotFactory snapshotFactory = new DanmakuSnapshotFactory();
    private final DanmakuSyncService syncService = new DanmakuSyncService();
    private final AtomicInteger nextId = new AtomicInteger(0);

    private DanmakuManager() {}

    public static DanmakuManager getInstance() {
        return INSTANCE;
    }

    private Map<Integer, Bullet> getWorldMap(World world) {
        return bulletStore.getOrCreateWorldEntries(world);
    }

    private Map<Integer, Laser> getLaserWorldMap(World world) {
        return laserStore.getOrCreateWorldEntries(world);
    }

    public int spawnBullet(World world, Vec3d position, Vec3d velocity, int life, float damage,
                           String texture, int color, float size, String rendererType,
                           NBTTagCompound customData, ICollisionShape collisionShape,
                           IBulletHitBehavior hitBehavior,
                           IBulletMotionController motionController, Consumer<IBulletActor> tickCallback,
                           IBulletCollisionFilter collisionFilter, boolean onlyPlayer,
                           EntityLivingBase shooter, ItemStack shooterHeldItem,
                           AttackSourceInfo attackSourceInfo, String renderPresetId) {
        if (world.isRemote) {
            return -1;
        }
        int id = nextId.getAndIncrement();
        Bullet bullet = new Bullet(id, position, velocity, life, damage,
                texture, color, size, rendererType, customData,
                collisionShape, hitBehavior, motionController, tickCallback, collisionFilter, onlyPlayer,
                shooter, shooterHeldItem, attackSourceInfo, renderPresetId);
        bulletStore.put(world, bullet);
        MinecraftForge.EVENT_BUS.post(new BulletSpawnEvent(world, createBulletSnapshot(bullet)));
        syncService.sendBulletSpawn(world, bullet);
        return id;
    }

    public void removeBullet(World world, int id) {
        removeBullet(world, id, LifecycleRemoveReason.API_REQUEST);
    }

    public void removeBullet(World world, int id, LifecycleRemoveReason reason) {
        Bullet removed = bulletStore.remove(world, id);
        if (removed == null) {
            return;
        }

        MinecraftForge.EVENT_BUS.post(new BulletRemoveEvent(world, createBulletSnapshot(removed), reason));
        syncService.sendBulletRemove(world, id);
    }

    public void updateBulletVelocity(World world, int id, Vec3d newVelocity) {
        Bullet bullet = getLiveBullet(world, id);
        if (bullet == null) {
            return;
        }
        bullet.setVelocity(newVelocity);
        syncBullet(world, bullet, SPacketDanmaku.FLAG_VELOCITY);
    }

    public void updateBulletPosition(World world, int id, Vec3d newPosition) {
        Bullet bullet = getLiveBullet(world, id);
        if (bullet == null) {
            return;
        }
        bullet.setPosition(newPosition);
        syncBullet(world, bullet, SPacketDanmaku.FLAG_POSITION);
    }

    public void updateBulletMotion(World world, int id, Vec3d newPosition, Vec3d newVelocity) {
        Bullet bullet = getLiveBullet(world, id);
        if (bullet == null) {
            return;
        }
        bullet.setPosition(newPosition);
        bullet.setVelocity(newVelocity);
        syncBullet(world, bullet, SPacketDanmaku.FLAG_POSITION | SPacketDanmaku.FLAG_VELOCITY);
    }

    public void updateBulletLife(World world, int id, int life) {
        Bullet bullet = getLiveBullet(world, id);
        if (bullet == null) {
            return;
        }
        bullet.setLife(life);
        syncBullet(world, bullet, SPacketDanmaku.FLAG_LIFE);
    }

    public void updateBulletTexture(World world, int id, String texture) {
        updateBulletVisual(world, id, texture, null, null, SPacketBulletVisual.FLAG_TEXTURE);
    }

    public void updateBulletRendererType(World world, int id, String rendererType) {
        updateBulletVisual(world, id, null, rendererType, null, SPacketBulletVisual.FLAG_RENDERER);
    }

    public void updateBulletRenderState(World world, int id, String renderState) {
        updateBulletVisual(world, id, null, null, renderState, SPacketBulletVisual.FLAG_RENDER_STATE);
    }

    public void updateBulletVisual(World world, int id, String texture, String rendererType, String renderState, int flags) {
        Bullet bullet = getLiveBullet(world, id);
        if (bullet == null || flags == 0) {
            return;
        }
        if ((flags & SPacketBulletVisual.FLAG_TEXTURE) != 0) {
            bullet.setTexture(texture);
        }
        if ((flags & SPacketBulletVisual.FLAG_RENDERER) != 0) {
            bullet.setRendererType(rendererType);
        }
        if ((flags & SPacketBulletVisual.FLAG_RENDER_STATE) != 0) {
            bullet.setRenderState(renderState);
        }
        syncBulletVisual(world, bullet, flags);
    }

    public boolean hasBullet(World world, int id) {
        return bulletStore.hasLive(world, id);
    }

    public BulletSnapshot getBulletSnapshot(World world, int id) {
        return bulletStore.getLiveSnapshot(world, id, snapshotFactory.bulletSnapshots());
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
                          AttackSourceInfo attackSourceInfo, String renderPresetId) {
        if (world.isRemote) {
            return -1;
        }
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
                shooter, shooterHeldItem, attackSourceInfo, renderPresetId);
        laser.setCurrentLength(computeLaserLength(laser, world));
        laserStore.put(world, laser);
        MinecraftForge.EVENT_BUS.post(new LaserSpawnEvent(world, createLaserSnapshot(laser)));
        syncService.sendLaserSpawn(world, laser);
        return id;
    }

    public void removeLaser(World world, int id) {
        removeLaser(world, id, LifecycleRemoveReason.API_REQUEST);
    }

    public void removeLaser(World world, int id, LifecycleRemoveReason reason) {
        Laser removed = laserStore.remove(world, id);
        if (removed == null) {
            return;
        }

        MinecraftForge.EVENT_BUS.post(new LaserRemoveEvent(world, createLaserSnapshot(removed), reason));
        syncService.sendLaserRemove(world, id);
    }

    public boolean hasLaser(World world, int id) {
        return laserStore.hasLive(world, id);
    }

    public LaserSnapshot getLaserSnapshot(World world, int id) {
        return laserStore.getLiveSnapshot(world, id, snapshotFactory.laserSnapshots());
    }

    public boolean updateLaserTransform(World world, int id, Vec3d start, Vec3d direction) {
        Laser laser = getLiveLaser(world, id);
        if (laser == null || laser.isDead()) {
            return false;
        }

        laser.setStart(start);
        laser.setDirection(direction);
        laser.setCurrentLength(computeLaserLength(laser, world));
        syncLaser(world, laser, SPacketLaser.FLAG_START | SPacketLaser.FLAG_DIRECTION | SPacketLaser.FLAG_LENGTH);
        return true;
    }

    public boolean updateLaserLength(World world, int id, double length) {
        Laser laser = getLiveLaser(world, id);
        if (laser == null) {
            return false;
        }
        laser.setCurrentLength(Math.max(0.0D, Math.min(length, laser.getMaxLength())));
        syncLaser(world, laser, SPacketLaser.FLAG_LENGTH);
        return true;
    }

    public void updateLaserLife(World world, int id, int life) {
        Laser laser = getLiveLaser(world, id);
        if (laser == null) {
            return;
        }
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
        if (world.isRemote || event.phase != TickEvent.Phase.END) {
            return;
        }

        Map<Integer, Laser> laserMap = laserStore.getWorldEntries(world);
        if (laserMap != null) {
            for (Laser laser : laserMap.values()) {
                laser.captureSyncBaseline();
                laser.update(world);
            }

            List<Laser> lasers = new ArrayList<>(laserMap.values());
            for (Laser laser : lasers) {
                if (laser.isDead()) {
                    continue;
                }
                if (!laserStore.isCurrent(world, laser.getId(), laser)) {
                    continue;
                }

                Vec3d previousStart = laser.getSyncBaselineStart();
                Vec3d previousDirection = laser.getSyncBaselineDirection();
                double previousLength = laser.getSyncBaselineLength();
                double length = computeLaserLength(laser, world);
                laser.setCurrentLength(length);
                handleLaserCollision(laser, world);
                int laserFlags = 0;
                if (!sameVec(previousStart, laser.getStart())) {
                    laserFlags |= SPacketLaser.FLAG_START;
                }
                if (!sameVec(previousDirection, laser.getDirection())) {
                    laserFlags |= SPacketLaser.FLAG_DIRECTION;
                }
                if (!sameDouble(previousLength, laser.getCurrentLength())) {
                    laserFlags |= SPacketLaser.FLAG_LENGTH;
                }
                syncLaser(world, laser, laserFlags);
            }

            removeDeadLasers(world, laserMap, LifecycleRemoveReason.EXPIRED);
        }

        Map<Integer, Bullet> map = bulletStore.getWorldEntries(world);
        if (map != null) {
            for (Bullet bullet : map.values()) {
                bullet.update(world);
            }

            List<Bullet> bullets = new ArrayList<>(map.values());
            List<EntityLivingBase> fallbackEntities = null;

            for (Bullet bullet : bullets) {
                ICollisionShape shape = bullet.getCollisionShape();
                if (shape == null || bullet.isDead()) {
                    continue;
                }
                if (!bulletStore.isCurrent(world, bullet.getId(), bullet)) {
                    continue;
                }

                double posX = bullet.getPosX();
                double posY = bullet.getPosY();
                double posZ = bullet.getPosZ();
                if (bullet.isOnlyPlayer()) {
                    for (EntityPlayer player : world.playerEntities) {
                        if (player.isDead || player.capabilities.disableDamage) {
                            continue;
                        }
                        if (!canBulletCollide(world, bullet, player)) {
                            continue;
                        }
                        if (shape.checkCollision(posX, posY, posZ, player)) {
                            handleCollision(bullet, world, player);
                            if (bullet.isDead() || !bulletStore.isCurrent(world, bullet.getId(), bullet)) {
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
                        if (entity.isDead) {
                            continue;
                        }
                        if (!canBulletCollide(world, bullet, entity)) {
                            continue;
                        }
                        if (shape.checkCollision(posX, posY, posZ, entity)) {
                            handleCollision(bullet, world, entity);
                            if (bullet.isDead() || !bulletStore.isCurrent(world, bullet.getId(), bullet)) {
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
        if (entity == null || entity.isDead) {
            return false;
        }
        if (bullet.getShooter() == entity) {
            return false;
        }

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
        Map<Integer, Bullet> removedBullets = bulletStore.removeWorld(world);
        if (removedBullets != null) {
            for (Bullet bullet : removedBullets.values()) {
                MinecraftForge.EVENT_BUS.post(new BulletRemoveEvent(world, createBulletSnapshot(bullet), LifecycleRemoveReason.WORLD_UNLOAD));
            }
        }

        Map<Integer, Laser> removedLasers = laserStore.removeWorld(world);
        if (removedLasers != null) {
            for (Laser laser : removedLasers.values()) {
                MinecraftForge.EVENT_BUS.post(new LaserRemoveEvent(world, createLaserSnapshot(laser), LifecycleRemoveReason.WORLD_UNLOAD));
            }
        }
    }

    public int getBulletCount(World world) {
        return bulletStore.countLive(world);
    }

    public int getLaserCount(World world) {
        return laserStore.countLive(world);
    }

    public List<Integer> getBulletIds(World world) {
        return bulletStore.getLiveIds(world);
    }

    public List<Integer> getLaserIds(World world) {
        return laserStore.getLiveIds(world);
    }

    public List<BulletSnapshot> getBulletSnapshots(World world) {
        return bulletStore.getLiveSnapshots(world, snapshotFactory.bulletSnapshots());
    }

    public List<LaserSnapshot> getLaserSnapshots(World world) {
        return laserStore.getLiveSnapshots(world, snapshotFactory.laserSnapshots());
    }

    private double computeLaserLength(Laser laser, World world) {
        double maxLength = laser.getMaxLength();
        if (!laser.isBlockStops()) {
            return maxLength;
        }
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
        if (length <= 0.0D) {
            return;
        }
        Vec3d end = start.add(dir.scale(length));

        AxisAlignedBB searchBox = buildSegmentAabb(start, end, laser.getThickness());
        List<EntityLivingBase> candidates = world.getEntitiesWithinAABB(EntityLivingBase.class, searchBox);

        long worldTick = world.getTotalWorldTime();
        if (!laser.isPenetrate()) {
            EntityLivingBase closest = null;
            double closestT = Double.MAX_VALUE;
            for (EntityLivingBase entity : candidates) {
                if (entity.isDead) {
                    continue;
                }
                if (!canLaserCollide(world, laser, entity)) {
                    continue;
                }
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
            if (entity.isDead) {
                continue;
            }
            if (!canLaserCollide(world, laser, entity)) {
                continue;
            }
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
        if (laser.getShooter() == entity) {
            return false;
        }
        AttackSourceInfo source = laser.getAttackSourceInfo();
        if (source != null && source.getOwnerId() != null && source.getOwnerId().equals(entity.getUniqueID())) {
            return false;
        }
        if (laser.isOnlyPlayer() && !(entity instanceof EntityPlayer)) {
            return false;
        }
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
        return snapshotFactory.createBulletSnapshot(bullet);
    }

    private LaserSnapshot createLaserSnapshot(Laser laser) {
        return snapshotFactory.createLaserSnapshot(laser);
    }

    private Bullet getLiveBullet(World world, int id) {
        return bulletStore.getLive(world, id);
    }

    private Laser getLiveLaser(World world, int id) {
        return laserStore.getLive(world, id);
    }

    private void syncBullet(World world, Bullet bullet, int flags) {
        if (bullet == null) {
            return;
        }
        if (bullet.isDead() || bullet.getLife() <= 0) {
            removeBullet(world, bullet.getId(), LifecycleRemoveReason.API_REQUEST);
            return;
        }
        syncService.syncBullet(world, bullet, flags);
    }

    private void syncLaser(World world, Laser laser, int flags) {
        if (laser == null) {
            return;
        }
        if (laser.isDead() || laser.getLife() == 0) {
            removeLaser(world, laser.getId(), LifecycleRemoveReason.API_REQUEST);
            return;
        }
        syncService.syncLaser(world, laser, flags);
    }

    private void syncBulletVisual(World world, Bullet bullet, int flags) {
        if (bullet == null) {
            return;
        }
        syncService.syncBulletVisual(world, bullet, flags);
    }

    private static boolean sameVec(Vec3d a, Vec3d b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
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
        if (local == null) {
            return new Vec3d(0, 0, 0);
        }
        if (look == null || look.lengthSquared() < 1.0E-6) {
            return new Vec3d(0, 0, 0);
        }
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
            if (start.x < box.minX || start.x > box.maxX) {
                return Double.NaN;
            }
        } else {
            double inv = 1.0D / dx;
            double t1 = (box.minX - start.x) * inv;
            double t2 = (box.maxX - start.x) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) {
                return Double.NaN;
            }
        }

        if (Math.abs(dy) < 1.0E-8) {
            if (start.y < box.minY || start.y > box.maxY) {
                return Double.NaN;
            }
        } else {
            double inv = 1.0D / dy;
            double t1 = (box.minY - start.y) * inv;
            double t2 = (box.maxY - start.y) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) {
                return Double.NaN;
            }
        }

        if (Math.abs(dz) < 1.0E-8) {
            if (start.z < box.minZ || start.z > box.maxZ) {
                return Double.NaN;
            }
        } else {
            double inv = 1.0D / dz;
            double t1 = (box.minZ - start.z) * inv;
            double t2 = (box.maxZ - start.z) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) {
                return Double.NaN;
            }
        }

        return tmin;
    }
}
