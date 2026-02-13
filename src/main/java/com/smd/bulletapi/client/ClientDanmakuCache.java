package com.smd.bulletapi.client;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SideOnly(Side.CLIENT)
public class ClientDanmakuCache {
    public static ClientDanmakuCache INSTANCE;

    private final Map<Integer, ClientBullet> bullets = new ConcurrentHashMap<>();

    public ClientDanmakuCache() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    // 新增参数
    public void spawnBullet(int id, Vec3d position, Vec3d velocity, int maxLife, float damage,
                            ResourceLocation texture, NBTTagCompound customData) {
        bullets.put(id, new ClientBullet(id, position, velocity, maxLife, damage, texture, customData));
    }

    public void updateBulletVelocity(int id, Vec3d velocity) {
        ClientBullet bullet = bullets.get(id);
        if (bullet != null) bullet.setVelocity(velocity);
    }

    public void removeBullet(int id) {
        bullets.remove(id);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (ClientBullet bullet : bullets.values()) {
                bullet.tick();
            }
            bullets.entrySet().removeIf(entry -> entry.getValue().isDead());
        }
    }

    public Map<Integer, ClientBullet> getBullets() {
        return bullets;
    }
}