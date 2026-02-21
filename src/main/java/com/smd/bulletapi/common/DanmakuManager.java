package com.smd.bulletapi.common;

import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.event.BulletCollisionEvent;
import com.smd.bulletapi.network.PacketHandler;
import com.smd.bulletapi.network.SPacketDanmaku;
import com.smd.bulletapi.server.Bullet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

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

    /**
     * 生成弹幕并返回其唯一ID
     */
    public int spawnBullet(World world, Vec3d position, Vec3d velocity, int life, float damage,
                           String texture, int color, float size, String rendererType,
                           NBTTagCompound customData, ICollisionShape collisionShape,
                           Consumer<CollisionContext> onCollision, Consumer<Bullet> tickCallback,
                           boolean onlyPlayer) {
        if (world.isRemote) return -1;
        int id = nextId.getAndIncrement();
        Bullet bullet = new Bullet(id, position, velocity, life, damage,
                texture, color, size, rendererType, customData,
                collisionShape, onCollision, tickCallback, onlyPlayer);
        getWorldMap(world).put(id, bullet);
        PacketHandler.sendToAll(SPacketDanmaku.createSpawn(
                id, position, velocity, life, damage, texture, color, size, rendererType, customData));
        return id;
    }

    public void removeBullet(World world, int id) {
        Map<Integer, Bullet> map = worldBullets.get(world);
        if (map != null) {
            map.remove(id);
            PacketHandler.sendToAll(SPacketDanmaku.createRemove(id));
        }
    }

    public void updateBulletVelocity(World world, int id, Vec3d newVelocity) {
        Map<Integer, Bullet> map = worldBullets.get(world);
        if (map != null) {
            Bullet bullet = map.get(id);
            if (bullet != null) {
                bullet.setVelocity(newVelocity);
                PacketHandler.sendToAll(SPacketDanmaku.createUpdate(id, newVelocity));
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
                // 更新所有弹幕位置
                for (Bullet bullet : map.values()) {
                    bullet.update(world);
                }

                // 碰撞检测
                for (Bullet bullet : map.values()) {
                    ICollisionShape shape = bullet.getCollisionShape();
                    if (shape == null || bullet.isDead()) continue;

                    Vec3d pos = bullet.getPosition();
                    if (bullet.isOnlyPlayer()) {
                        // 只检测玩家（玩家也是 EntityLivingBase）
                        for (EntityPlayer player : world.playerEntities) {
                            if (player.isDead || player.capabilities.disableDamage) continue;
                            if (shape.checkCollision(pos, player)) {
                                handleCollision(bullet, world, player);
                            }
                        }
                    } else {
                        // 检测所有 EntityLivingBase 实体
                        for (Entity entity : world.loadedEntityList) {
                            if (entity.isDead) continue;
                            if (entity instanceof EntityLivingBase) {
                                if (shape.checkCollision(pos, entity)) {
                                    handleCollision(bullet, world, entity);
                                }
                            }
                        }
                    }
                }

                // 移除已死亡的弹幕
                map.entrySet().removeIf(entry -> entry.getValue().isDead());
            }
        }
    }

    private void handleCollision(Bullet bullet, World world, Entity entity) {
        System.out.println("碰撞触发: 弹幕 " + bullet.getId() + " 击中 " + entity.getName());
        CollisionContext ctx = new CollisionContext(bullet, world, entity);
        BulletCollisionEvent eventBus = new BulletCollisionEvent(world, bullet, entity, ctx);
        MinecraftForge.EVENT_BUS.post(eventBus);
        if (!eventBus.isCanceled()) {
            bullet.onCollision(world, entity);
            if (!ctx.canceled) {
                entity.setDead();
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