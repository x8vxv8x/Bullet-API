package com.smd.bulletapi.common;

import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.event.BulletCollisionEvent;
import com.smd.bulletapi.network.PacketHandler;
import com.smd.bulletapi.network.SPacketDanmaku;
import com.smd.bulletapi.server.Bullet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
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
    private final AtomicInteger nextId = new AtomicInteger(0);

    private DanmakuManager() {}

    public static DanmakuManager getInstance() {
        return INSTANCE;
    }

    private Map<Integer, Bullet> getWorldMap(World world) {
        return worldBullets.computeIfAbsent(world, w -> new ConcurrentHashMap<>());
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

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        World world = event.world;
        if (world.isRemote) return;
        if (event.phase == TickEvent.Phase.END) {
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
    }

    public int getBulletCount(World world) {
        Map<Integer, Bullet> map = worldBullets.get(world);
        return map == null ? 0 : map.size();
    }
}
