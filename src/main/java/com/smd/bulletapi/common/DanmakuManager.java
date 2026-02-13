package com.smd.bulletapi.common;


import com.smd.bulletapi.network.PacketHandler;
import com.smd.bulletapi.network.SPacketDanmaku;
import com.smd.bulletapi.server.Bullet;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
        if (world.isRemote) return;
        int id = nextId.getAndIncrement();
        Bullet bullet = new Bullet(id, position, velocity, life, damage);
        getWorldMap(world).put(id, bullet);
        // 广播生成包
        PacketHandler.sendToAll(SPacketDanmaku.createSpawn(id, position, velocity, life, damage));
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
                // 移除已死亡的弹幕
                map.entrySet().removeIf(entry -> entry.getValue().isDead());
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        worldBullets.remove(event.getWorld());
    }
}
