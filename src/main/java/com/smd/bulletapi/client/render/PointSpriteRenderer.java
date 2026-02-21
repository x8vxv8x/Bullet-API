package com.smd.bulletapi.client.render;

import com.smd.bulletapi.client.ClientBullet;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class PointSpriteRenderer implements IBulletRenderer {
    public static final PointSpriteRenderer INSTANCE = new PointSpriteRenderer();
    private PointSpriteRenderer() {}

    private final double[] renderPos = new double[3];

    @Override
    public void render(ClientBullet bullet, float partialTicks, double viewX, double viewY, double viewZ) {
        bullet.getRenderPosition(partialTicks, renderPos);
        double x = renderPos[0] - viewX;
        double y = renderPos[1] - viewY;
        double z = renderPos[2] - viewZ;

        int color = bullet.getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        // 保存当前状态
        GlStateManager.disableTexture2D();
        GL11.glPointSize(bullet.getSize()); // 点大小
        GL11.glEnable(GL11.GL_POINT_SMOOTH); // 可选，使点更圆润

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_POINTS, DefaultVertexFormats.POSITION_COLOR);
        buf.pos(x, y, z).color(r, g, b, 1.0f).endVertex();
        tess.draw();

        // 恢复状态
        GL11.glDisable(GL11.GL_POINT_SMOOTH);
        GlStateManager.enableTexture2D();
    }
}
