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

@SideOnly(Side.CLIENT)
public class BillboardRenderer implements IBulletRenderer {
    public static final BillboardRenderer INSTANCE = new BillboardRenderer();
    private BillboardRenderer() {}

    // 复用临时位置数组
    private final double[] renderPos = new double[3];

    @Override
    public void render(ClientBullet bullet, float partialTicks, double viewX, double viewY, double viewZ) {
        // 获取插值位置
        bullet.getRenderPosition(partialTicks, renderPos);
        double x = renderPos[0] - viewX;
        double y = renderPos[1] - viewY;
        double z = renderPos[2] - viewZ;

        ResourceLocation texture = bullet.getTexture();
        if (texture == null) return; // 没有纹理则无法渲染

        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        float size = bullet.getSize();
        int color = bullet.getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        // 公告板计算：始终面向摄像机，使用相机右向量和上向量
        // 注意：此处使用简化方法（假设上向量始终为Y轴），若需更精确可计算相机矩阵
        // 这里采用与原来相同的算法（基于水平方向旋转）
        double dx = x;
        double dz = z;
        double f = Math.sqrt(dx * dx + dz * dz);
        float sinYaw, cosYaw;
        if (f < 1e-7) {
            sinYaw = 0f;
            cosYaw = 1f;
        } else {
            sinYaw = (float) (dx / f);
            cosYaw = (float) (dz / f);
        }

        double half = size / 2.0;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

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

        tess.draw();
    }
}