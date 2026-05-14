package com.smd.bulletapi.client.render;

import com.smd.bulletapi.client.ClientBullet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.Collections;

@SideOnly(Side.CLIENT)
public class BillboardRenderer implements IBulletRenderer {
    public static final BillboardRenderer INSTANCE = new BillboardRenderer();
    private BillboardRenderer() {}

    @Override
    public boolean canBatch() { return true; }

    @Override
    public void renderBatch(Collection<ClientBullet> bullets, float partialTicks, double viewX, double viewY, double viewZ) {
        if (bullets.isEmpty()) {
            return;
        }

        // 假设所有子弹使用相同纹理（由第一个子弹决定）
        ResourceLocation texture = bullets.iterator().next().getTexture();
        if (texture == null) {
            return;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        for (ClientBullet bullet : bullets) {
            double x = bullet.getRenderX(partialTicks) - viewX;
            double y = bullet.getRenderY(partialTicks) - viewY;
            double z = bullet.getRenderZ(partialTicks) - viewZ;

            float size = bullet.getSize();
            int color = bullet.getColor();
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            // 公告板计算（简化版，可使用相机向量优化）
            double half = size / 2.0;
            buf.pos(x - half, y - half, z).tex(0, 1).color(r, g, b, 1).endVertex();
            buf.pos(x + half, y - half, z).tex(1, 1).color(r, g, b, 1).endVertex();
            buf.pos(x + half, y + half, z).tex(1, 0).color(r, g, b, 1).endVertex();
            buf.pos(x - half, y + half, z).tex(0, 0).color(r, g, b, 1).endVertex();
        }

        tess.draw();
    }

    @Override
    public void render(ClientBullet bullet, float partialTicks, double viewX, double viewY, double viewZ) {
        // 单个渲染保留，用于不支持批量的情况或调试
        renderBatch(Collections.singleton(bullet), partialTicks, viewX, viewY, viewZ);
    }
}
