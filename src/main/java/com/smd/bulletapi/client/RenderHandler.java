package com.smd.bulletapi.client;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.client.render.IBulletRenderer;
import com.smd.bulletapi.client.render.ILaserRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SideOnly(Side.CLIENT)
@InternalApi
public class RenderHandler {
    private static final Map<IBulletRenderer, List<ClientBullet>> RENDER_GROUPS = new HashMap<>();
    private static final Map<ILaserRenderer, List<ClientLaser>> LASER_GROUPS = new HashMap<>();
    private static final ArrayDeque<List<ClientBullet>> BULLET_GROUP_POOL = new ArrayDeque<>();
    private static final ArrayDeque<List<ClientLaser>> LASER_GROUP_POOL = new ArrayDeque<>();

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) {
            recycleBulletGroups();
            recycleLaserGroups();
            return;
        }

        ClientDanmakuCache cache = ClientDanmakuCache.INSTANCE;
        ClientLaserCache laserCache = ClientLaserCache.INSTANCE;
        ClientSummonCache summonCache = ClientSummonCache.INSTANCE;
        boolean hasBullets = cache != null && !cache.getBullets().isEmpty();
        boolean hasSummons = summonCache != null && !summonCache.getSummons().isEmpty();
        boolean hasLasers = laserCache != null && !laserCache.getLasers().isEmpty();
        if (!hasBullets && !hasSummons && !hasLasers) {
            recycleBulletGroups();
            recycleLaserGroups();
            return;
        }

        float partialTicks = event.getPartialTicks();

        // 获取摄像机位置（用于坐标变换）
        double viewX = mc.player.prevPosX + (mc.player.posX - mc.player.prevPosX) * partialTicks;
        double viewY = mc.player.prevPosY + (mc.player.posY - mc.player.prevPosY) * partialTicks;
        double viewZ = mc.player.prevPosZ + (mc.player.posZ - mc.player.prevPosZ) * partialTicks;

        recycleBulletGroups();
        if (hasBullets || hasSummons) {
            if (hasBullets) {
                for (ClientBullet bullet : cache.getBullets().values()) {
                    if (bullet.isDead()) continue;
                    IBulletRenderer renderer = bullet.getRenderer();
                    if (renderer != null) {
                        acquireBulletGroup(renderer).add(bullet);
                    }
                }
            }
            if (hasSummons) {
                for (ClientBullet bullet : summonCache.getSummons().values()) {
                    if (bullet.isDead()) continue;
                    IBulletRenderer renderer = bullet.getRenderer();
                    if (renderer != null) {
                        acquireBulletGroup(renderer).add(bullet);
                    }
                }
            }
        }

        // 设置通用渲染状态（所有渲染器共享）
        GlStateManager.pushMatrix();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false); // 允许透明叠加，根据需求可选

        // 遍历每个渲染器组（弹幕）
        if (hasBullets || hasSummons) {
            for (Map.Entry<IBulletRenderer, List<ClientBullet>> entry : RENDER_GROUPS.entrySet()) {
                IBulletRenderer renderer = entry.getKey();
                List<ClientBullet> bullets = entry.getValue();
                if (bullets.isEmpty()) continue;

                renderer.beginRender();
                try {
                    if (renderer.canBatch()) {
                        // 支持批量渲染的渲染器一次性提交所有子弹
                        renderer.renderBatch(bullets, partialTicks, viewX, viewY, viewZ);
                    } else {
                        // 不支持批量的渲染器逐个渲染
                        for (ClientBullet bullet : bullets) {
                            renderer.render(bullet, partialTicks, viewX, viewY, viewZ);
                        }
                    }
                } finally {
                    renderer.endRender();
                }
            }
        }

        // 渲染激光
        recycleLaserGroups();
        if (hasLasers) {
            for (ClientLaser laser : laserCache.getLasers().values()) {
                ILaserRenderer renderer = laser.getRenderer();
                if (renderer != null) {
                    acquireLaserGroup(renderer).add(laser);
                }
            }

            for (Map.Entry<ILaserRenderer, List<ClientLaser>> entry : LASER_GROUPS.entrySet()) {
                ILaserRenderer renderer = entry.getKey();
                List<ClientLaser> lasers = entry.getValue();
                if (lasers.isEmpty()) continue;
                renderer.beginRender();
                try {
                    if (renderer.canBatch()) {
                        renderer.renderBatch(lasers, partialTicks, viewX, viewY, viewZ);
                    } else {
                        for (ClientLaser laser : lasers) {
                            renderer.render(laser, partialTicks, viewX, viewY, viewZ);
                        }
                    }
                } finally {
                    renderer.endRender();
                }
            }
        }

        // 恢复渲染状态
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static List<ClientBullet> acquireBulletGroup(IBulletRenderer renderer) {
        List<ClientBullet> group = RENDER_GROUPS.get(renderer);
        if (group != null) {
            return group;
        }
        group = BULLET_GROUP_POOL.pollFirst();
        if (group == null) {
            group = new ArrayList<>();
        }
        RENDER_GROUPS.put(renderer, group);
        return group;
    }

    private static List<ClientLaser> acquireLaserGroup(ILaserRenderer renderer) {
        List<ClientLaser> group = LASER_GROUPS.get(renderer);
        if (group != null) {
            return group;
        }
        group = LASER_GROUP_POOL.pollFirst();
        if (group == null) {
            group = new ArrayList<>();
        }
        LASER_GROUPS.put(renderer, group);
        return group;
    }

    private static void recycleBulletGroups() {
        for (List<ClientBullet> group : RENDER_GROUPS.values()) {
            group.clear();
            BULLET_GROUP_POOL.addLast(group);
        }
        RENDER_GROUPS.clear();
    }

    private static void recycleLaserGroups() {
        for (List<ClientLaser> group : LASER_GROUPS.values()) {
            group.clear();
            LASER_GROUP_POOL.addLast(group);
        }
        LASER_GROUPS.clear();
    }
}
