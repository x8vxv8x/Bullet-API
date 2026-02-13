package com.smd.bulletapi.common;


import com.smd.bulletapi.common.collision.CollisionHelper;
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

    public void spawnBullet(World world, Vec3d position, Vec3d velocity, int life, float damage) {
        spawnBullet(world, position, velocity, life, damage, null, null, null, null);
    }

    // 完整版
    public void spawnBullet(World world, Vec3d position, Vec3d velocity, int life, float damage,
                            String texture, NBTTagCompound customData,
                            ICollisionShape collisionShape, Consumer<CollisionContext> onCollision) {
        if (world.isRemote) return;
        int id = nextId.getAndIncrement();
        Bullet bullet = new Bullet(id, position, velocity, life, damage, texture, customData, collisionShape, onCollision);
        getWorldMap(world).put(id, bullet);
        PacketHandler.sendToAll(SPacketDanmaku.createSpawn(
                id, position, velocity, life, damage, texture, customData));
        // 注意：碰撞盒和回调是服务端专用的，**不同步到客户端**（客户端无需碰撞逻辑）
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
                for (Bullet bullet : map.values()) {
                    bullet.update(world);
                }
                List<EntityPlayer> players = world.playerEntities;
                for (Bullet bullet : map.values()) {
                    if (!bullet.hasCollision() || bullet.isDead()) continue;

                    Vec3d pos = bullet.getPosition();
                    for (EntityPlayer player : players) {
                        if (player.isDead || player.capabilities.disableDamage) continue;
                        if (CollisionHelper.checkCollision(bullet.getCollisionShape(), pos, player)) {
                            // 触发碰撞
                            CollisionContext ctx = new CollisionContext(bullet, world, player);
                            // 1. 触发事件（便于其他Mod监听）
                            BulletCollisionEvent Bulletevent = new BulletCollisionEvent(world, bullet, player, ctx);
                            MinecraftForge.EVENT_BUS.post(Bulletevent);
                            if (!Bulletevent.isCanceled()) {
                                // 2. 调用弹幕自身的回调
                                bullet.onCollision(world, player);
                                // 3. 默认伤害处理（若未被取消）
                                if (!ctx.canceled) {
                                    player.attackEntityFrom(DamageSource.GENERIC, ctx.damage);
                                }
                            }
                            // 碰撞后弹幕是否消失？可由开发者通过回调控制，默认不消失。
                            // 如需消失，可在回调中调用 DanmakuManager.removeBullet
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
