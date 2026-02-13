package com.smd.bulletapi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.*;

@SideOnly(Side.CLIENT)
public class RenderHandler {

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        ClientDanmakuCache cache = ClientDanmakuCache.INSTANCE;
        if (cache == null || cache.getBullets().isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        float partialTicks = event.getPartialTicks();

        double viewX = mc.player.prevPosX + (mc.player.posX - mc.player.prevPosX) * partialTicks;
        double viewY = mc.player.prevPosY + (mc.player.posY - mc.player.prevPosY) * partialTicks;
        double viewZ = mc.player.prevPosZ + (mc.player.posZ - mc.player.prevPosZ) * partialTicks;

        // 1. 分离有纹理和无纹理的弹幕
        List<ClientBullet> texturedBullets = new ArrayList<>();
        List<ClientBullet> pointBullets = new ArrayList<>();

        for (ClientBullet bullet : cache.getBullets().values()) {
            if (bullet.getTexture() != null) {
                texturedBullets.add(bullet);
            } else {
                pointBullets.add(bullet);
            }
        }

        GlStateManager.pushMatrix();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth(); // 避免被方块遮挡

        // ---------- 绘制点精灵（无纹理） ----------
        if (!pointBullets.isEmpty()) {
            GlStateManager.disableTexture2D();
            GL11.glPointSize(6.0F);
            GL11.glEnable(GL11.GL_POINT_SMOOTH);

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            buf.begin(GL11.GL_POINTS, DefaultVertexFormats.POSITION_COLOR);

            for (ClientBullet bullet : pointBullets) {
                Vec3d pos = bullet.getRenderPosition(partialTicks);
                double x = pos.x - viewX;
                double y = pos.y - viewY;
                double z = pos.z - viewZ;
                // 从 customData 读取颜色，若没有则默认红色
                int color = getColorFromCustomData(bullet.getCustomData(), 0xFF5555);
                float r = ((color >> 16) & 0xFF) / 255f;
                float g = ((color >> 8) & 0xFF) / 255f;
                float b = (color & 0xFF) / 255f;
                buf.pos(x, y, z).color(r, g, b, 1.0F).endVertex();
            }
            tess.draw();
            GL11.glDisable(GL11.GL_POINT_SMOOTH);
        }

        // ---------- 绘制带纹理弹幕（分组批量） ----------
        if (!texturedBullets.isEmpty()) {
            GlStateManager.enableTexture2D();

            // 按纹理分组
            Map<ResourceLocation, List<ClientBullet>> grouped = new HashMap<>();
            for (ClientBullet bullet : texturedBullets) {
                grouped.computeIfAbsent(bullet.getTexture(), k -> new ArrayList<>()).add(bullet);
            }

            Tessellator tess = Tessellator.getInstance();
            for (Map.Entry<ResourceLocation, List<ClientBullet>> entry : grouped.entrySet()) {
                ResourceLocation tex = entry.getKey();
                List<ClientBullet> list = entry.getValue();

                // 绑定纹理
                mc.getTextureManager().bindTexture(tex);

                BufferBuilder buf = tess.getBuffer();
                buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

                for (ClientBullet bullet : list) {
                    Vec3d pos = bullet.getRenderPosition(partialTicks);
                    double x = pos.x - viewX;
                    double y = pos.y - viewY;
                    double z = pos.z - viewZ;

                    // 从 customData 获取大小，默认 0.5
                    float size = getSizeFromCustomData(bullet.getCustomData(), 0.5f);
                    // 获取颜色（用于叠加，默认为白色）
                    int color = getColorFromCustomData(bullet.getCustomData(), 0xFFFFFF);
                    float r = ((color >> 16) & 0xFF) / 255f;
                    float g = ((color >> 8) & 0xFF) / 255f;
                    float b = (color & 0xFF) / 255f;

                    // 构建面向玩家的四边形（公告板）
                    // 计算垂直向量：Y轴固定，水平面始终面向摄像机
                    double dx = x; // 相对于视图的位置
                    double dy = y;
                    double dz = z;
                    double f = Math.sqrt(dx * dx + dz * dz);
                    // 避免除以零
                    float sinYaw = (float) (dx / f);
                    float cosYaw = (float) (dz / f);

                    // 四个顶点（相对于弹幕中心）
                    double half = size / 2.0;
                    // 左下
                    buf.pos(x - half * cosYaw, y - half, z - half * sinYaw)
                            .tex(0, 1).color(r, g, b, 1.0f).endVertex();
                    // 右下
                    buf.pos(x + half * cosYaw, y - half, z + half * sinYaw)
                            .tex(1, 1).color(r, g, b, 1.0f).endVertex();
                    // 右上
                    buf.pos(x + half * cosYaw, y + half, z + half * sinYaw)
                            .tex(1, 0).color(r, g, b, 1.0f).endVertex();
                    // 左上
                    buf.pos(x - half * cosYaw, y + half, z - half * sinYaw)
                            .tex(0, 0).color(r, g, b, 1.0f).endVertex();
                }
                tess.draw();
            }
        }

        // 恢复渲染状态
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    // 辅助方法：从 customData 读取颜色（默认值）
    private static int getColorFromCustomData(NBTTagCompound data, int defaultColor) {
        if (data != null && data.hasKey("Color")) {
            return data.getInteger("Color");
        }
        return defaultColor;
    }

    // 辅助方法：从 customData 读取尺寸（默认值）
    private static float getSizeFromCustomData(NBTTagCompound data, float defaultSize) {
        if (data != null && data.hasKey("Size")) {
            return data.getFloat("Size");
        }
        return defaultSize;
    }
}