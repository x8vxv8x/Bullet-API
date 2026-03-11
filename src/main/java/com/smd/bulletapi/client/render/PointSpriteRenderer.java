package com.smd.bulletapi.client.render;

import com.smd.bulletapi.client.ClientBullet;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.Collections;

@SideOnly(Side.CLIENT)
public class PointSpriteRenderer implements IBulletRenderer {
    public static final PointSpriteRenderer INSTANCE = new PointSpriteRenderer();
    private PointSpriteRenderer() {}

    @Override
    public boolean canBatch() { return true; }

    @Override
    public void renderBatch(Collection<ClientBullet> bullets, float partialTicks, double viewX, double viewY, double viewZ) {
        GlStateManager.disableTexture2D();
        GL11.glPointSize(6.0F);
        GL11.glEnable(GL11.GL_POINT_SMOOTH);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_POINTS, DefaultVertexFormats.POSITION_COLOR);

        for (ClientBullet bullet : bullets) {
            double x = bullet.getRenderX(partialTicks) - viewX;
            double y = bullet.getRenderY(partialTicks) - viewY;
            double z = bullet.getRenderZ(partialTicks) - viewZ;

            int color = bullet.getColor();
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            buf.pos(x, y, z).color(r, g, b, 1.0F).endVertex();
        }

        tess.draw();
        GL11.glDisable(GL11.GL_POINT_SMOOTH);
        GlStateManager.enableTexture2D();
    }

    @Override
    public void render(ClientBullet bullet, float partialTicks, double viewX, double viewY, double viewZ) {
        renderBatch(Collections.singleton(bullet), partialTicks, viewX, viewY, viewZ);
    }
}
