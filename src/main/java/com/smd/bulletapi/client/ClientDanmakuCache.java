package com.smd.bulletapi.client;

import com.smd.bulletapi.client.render.IBulletRenderer;
import com.smd.bulletapi.client.render.BillboardRenderer;
import com.smd.bulletapi.client.render.PointSpriteRenderer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@SideOnly(Side.CLIENT)
public class ClientDanmakuCache {
    public static ClientDanmakuCache INSTANCE;

    private final Map<Integer, ClientBullet> bullets = new ConcurrentHashMap<>();

    // 渲染器工厂映射
    private static final Map<String, Function<NBTTagCompound, IBulletRenderer>> RENDERER_FACTORIES = new HashMap<>();

    static {
        // 注册内置渲染器
        RENDERER_FACTORIES.put("billboard", data -> BillboardRenderer.INSTANCE);
        RENDERER_FACTORIES.put("point", data -> PointSpriteRenderer.INSTANCE);
        // 可在此扩展其他渲染器，如 "entity_model", "obj_model"
    }

    public ClientDanmakuCache() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    public void spawnBullet(int id, Vec3d position, Vec3d velocity, int maxLife, float damage,
                            ResourceLocation texture, NBTTagCompound customData) {
        // 创建子弹实例
        ClientBullet bullet = new ClientBullet(id, position, velocity, maxLife, damage, texture, customData);

        // 确定渲染器类型
        String rendererType = null;
        if (customData != null && customData.hasKey("RendererType")) {
            rendererType = customData.getString("RendererType");
        }
        if (rendererType == null) {
            // 根据纹理自动选择：有纹理用公告板，否则用点精灵
            rendererType = (texture != null) ? "billboard" : "point";
        }

        // 获取渲染器
        Function<NBTTagCompound, IBulletRenderer> factory = RENDERER_FACTORIES.get(rendererType);
        if (factory != null) {
            bullet.setRenderer(factory.apply(customData));
        } else {
            // 回退：按纹理选择
            bullet.setRenderer((texture != null) ? BillboardRenderer.INSTANCE : PointSpriteRenderer.INSTANCE);
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
            bullet.getRenderer().deleteGlResources(); // 释放渲染器资源
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (ClientBullet bullet : bullets.values()) {
                bullet.tick();
            }
            // 移除已死亡子弹并释放资源
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