package com.smd.bulletapi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class RenderHandler {

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        ClientDanmakuCache cache = ClientDanmakuCache.INSTANCE;
        if (cache == null || cache.getBullets().isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        float partialTicks = event.getPartialTicks();

        // 获取摄像机位置（插值）
        double viewX = mc.player.prevPosX + (mc.player.posX - mc.player.prevPosX) * partialTicks;
        double viewY = mc.player.prevPosY + (mc.player.posY - mc.player.prevPosY) * partialTicks;
        double viewZ = mc.player.prevPosZ + (mc.player.posZ - mc.player.prevPosZ) * partialTicks;

        // 设置GL状态
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();

        GL11.glPointSize(6.0F);
        GL11.glEnable(GL11.GL_POINT_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_POINTS, DefaultVertexFormats.POSITION_COLOR);

        for (ClientBullet bullet : cache.getBullets().values()) {
            Vec3d renderPos = bullet.getRenderPosition(partialTicks);
            double x = renderPos.x - viewX;
            double y = renderPos.y - viewY;
            double z = renderPos.z - viewZ;
            // 红色弹幕，完全透明
            buffer.pos(x, y, z).color(1.0F, 0.2F, 0.2F, 1.0F).endVertex();
        }

        tessellator.draw();

        GL11.glDisable(GL11.GL_POINT_SMOOTH);
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
}
