package com.smd.bulletapi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
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

        // 分离有纹理和无纹理的弹幕
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

        // 复用数组，避免每颗子弹创建 Vec3d
        double[] renderPos = new double[3];

        // ---------- 绘制点精灵（无纹理） ----------
        if (!pointBullets.isEmpty()) {
            GlStateManager.disableTexture2D();
            GL11.glPointSize(6.0F);
            GL11.glEnable(GL11.GL_POINT_SMOOTH);

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            buf.begin(GL11.GL_POINTS, DefaultVertexFormats.POSITION_COLOR);

            for (ClientBullet bullet : pointBullets) {
                bullet.getRenderPosition(partialTicks, renderPos);
                double x = renderPos[0] - viewX;
                double y = renderPos[1] - viewY;
                double z = renderPos[2] - viewZ;

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

                mc.getTextureManager().bindTexture(tex);

                BufferBuilder buf = tess.getBuffer();
                buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

                for (ClientBullet bullet : list) {
                    bullet.getRenderPosition(partialTicks, renderPos);
                    double x = renderPos[0] - viewX;
                    double y = renderPos[1] - viewY;
                    double z = renderPos[2] - viewZ;

                    float size = getSizeFromCustomData(bullet.getCustomData(), 0.5f);
                    int color = getColorFromCustomData(bullet.getCustomData(), 0xFFFFFF);
                    float r = ((color >> 16) & 0xFF) / 255f;
                    float g = ((color >> 8) & 0xFF) / 255f;
                    float b = (color & 0xFF) / 255f;

                    // 公告板计算（始终面向摄像机）
                    double dx = x;
                    double dz = z;
                    double f = Math.sqrt(dx * dx + dz * dz);
                    float sinYaw, cosYaw;
                    if (f < 1e-7) { // 当子弹正上方/正下方时，任意朝向均可，默认朝Z
                        sinYaw = 0f;
                        cosYaw = 1f;
                    } else {
                        sinYaw = (float) (dx / f);
                        cosYaw = (float) (dz / f);
                    }

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