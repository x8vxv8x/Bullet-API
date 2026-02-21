package com.smd.bulletapi.common;

import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.event.BulletCollisionEvent;
import com.smd.bulletapi.network.PacketHandler;
import com.smd.bulletapi.network.SPacketDanmaku;
import com.smd.bulletapi.server.Bullet;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;
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
                           String texture, NBTTagCompound customData,
                           ICollisionShape collisionShape, Consumer<CollisionContext> onCollision) {
        if (world.isRemote) return -1;
        int id = nextId.getAndIncrement();
        Bullet bullet = new Bullet(id, position, velocity, life, damage, texture, customData, collisionShape, onCollision);
        getWorldMap(world).put(id, bullet);
        PacketHandler.sendToAll(SPacketDanmaku.createSpawn(
                id, position, velocity, life, damage, texture, customData));
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
                List<EntityPlayer> players = world.playerEntities;
                for (Bullet bullet : map.values()) {
                    ICollisionShape shape = bullet.getCollisionShape();
                    if (shape == null || bullet.isDead()) continue;

                    Vec3d pos = bullet.getPosition();
                    for (EntityPlayer player : players) {
                        if (player.isDead || player.capabilities.disableDamage) continue;

                        // 使用形状的统一碰撞检查（包含快速过滤）
                        if (shape.checkCollision(pos, player)) {
                            CollisionContext ctx = new CollisionContext(bullet, world, player);
                            BulletCollisionEvent eventBus = new BulletCollisionEvent(world, bullet, player, ctx);
                            MinecraftForge.EVENT_BUS.post(eventBus);
                            if (!eventBus.isCanceled()) {
                                bullet.onCollision(world, player);
                                if (!ctx.canceled) {
                                    player.attackEntityFrom(DamageSource.GENERIC, ctx.damage);
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

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        worldBullets.remove(event.getWorld());
    }

    public int getBulletCount(World world) {
        Map<Integer, Bullet> map = worldBullets.get(world);
        return map == null ? 0 : map.size();
    }
}