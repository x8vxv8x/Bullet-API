package com.smd.bulletapi.client;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.client.render.BillboardRenderer;
import com.smd.bulletapi.client.render.IBulletRenderer;
import com.smd.bulletapi.client.render.PointSpriteRenderer;
import com.smd.bulletapi.client.render.RendererRegistry;
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
@InternalApi
public class ClientDanmakuCache {
    public static ClientDanmakuCache INSTANCE;

    private final Map<Integer, ClientBullet> bullets = new ConcurrentHashMap<>();

    public ClientDanmakuCache() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    public void spawnBullet(int id, Vec3d position, Vec3d velocity, int maxLife, float damage,
                            ResourceLocation texture, int color, float size, String rendererType,
                            NBTTagCompound customData) {
        ClientBullet bullet = new ClientBullet(id, position, velocity, maxLife, damage,
                texture, color, size, rendererType, customData);

        // 根据 rendererType 创建渲染器（若未指定或注册失败，回退逻辑）
        if (rendererType != null && RendererRegistry.hasType(rendererType)) {
            bullet.setRenderer(RendererRegistry.create(rendererType, customData));
        } else {
            // 自动回退：有纹理用公告板，否则用点精灵
            bullet.setRenderer(texture != null ? BillboardRenderer.INSTANCE : PointSpriteRenderer.INSTANCE);
        }

        bullets.put(id, bullet);
    }

    public void updateBulletVelocity(int id, Vec3d velocity) {
        ClientBullet bullet = bullets.get(id);
        if (bullet != null) bullet.setVelocity(velocity);
    }

    public void removeBullet(int id) {
        ClientBullet bullet = bullets.remove(id);
        if (bullet != null && bullet.getRenderer() != null) {
            bullet.getRenderer().deleteGlResources();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (ClientBullet bullet : bullets.values()) {
                bullet.tick();
            }
            bullets.entrySet().removeIf(entry -> {
                if (entry.getValue().isDead()) {
                    IBulletRenderer renderer = entry.getValue().getRenderer();
                    if (renderer != null) renderer.deleteGlResources();
                    return true;
                }
                return false;
            });
        }
    }

    public Map<Integer, ClientBullet> getBullets() {
        return bullets;
    }
}
