package com.smd.bulletapi.client.render;

import com.smd.bulletapi.client.ClientLaser;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Collection;

@SideOnly(Side.CLIENT)
public class LaserBeamRenderer implements ILaserRenderer {
    public static final LaserBeamRenderer INSTANCE = new LaserBeamRenderer();
    private LaserBeamRenderer() {}

    @Override
    public void beginRender() {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
    }

    @Override
    public void endRender() {
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
    }

    @Override
    public void renderBatch(Collection<ClientLaser> lasers, float partialTicks, double viewX, double viewY, double viewZ) {
        if (lasers.isEmpty()) {
            return;
        }

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        for (ClientLaser laser : lasers) {
            Vec3d start = laser.getRenderStart(partialTicks);
            Vec3d end = laser.getRenderEnd(partialTicks);

            Vec3d dir = end.subtract(start);
            if (dir.lengthSquared() < 1.0E-6) {
                continue;
            }
            dir = dir.normalize();

            Vec3d viewDir = new Vec3d(viewX - start.x, viewY - start.y, viewZ - start.z);
            if (viewDir.lengthSquared() < 1.0E-6) {
                viewDir = new Vec3d(0, 1, 0);
            }
            viewDir = viewDir.normalize();

            Vec3d right = dir.crossProduct(viewDir);
            if (right.lengthSquared() < 1.0E-6) {
                right = dir.crossProduct(new Vec3d(0, 1, 0));
            }
            if (right.lengthSquared() < 1.0E-6) {
                right = new Vec3d(1, 0, 0);
            }
            right = right.normalize().scale(laser.getThickness() * 0.5);

            Vec3d s = new Vec3d(start.x - viewX, start.y - viewY, start.z - viewZ);
            Vec3d e = new Vec3d(end.x - viewX, end.y - viewY, end.z - viewZ);

            Vec3d s1 = s.add(right);
            Vec3d s2 = s.subtract(right);
            Vec3d e1 = e.add(right);
            Vec3d e2 = e.subtract(right);

            int color = laser.getColor();
            NBTTagCompound data = laser.getCustomData();
            float a = data != null && data.hasKey("alpha") ? data.getFloat("alpha") : 0.85f;
            boolean useHelper = data != null && data.hasKey("use_helper") && data.getBoolean("use_helper");
            if (useHelper) {
                int colorEnd = data.hasKey("color_end") ? data.getInteger("color_end") : color;
                LaserRenderHelper.emitQuad(buf, s1, s2, e2, e1, color, colorEnd, a);
            } else {
                float r = ((color >> 16) & 0xFF) / 255f;
                float g = ((color >> 8) & 0xFF) / 255f;
                float b = (color & 0xFF) / 255f;
                buf.pos(s1.x, s1.y, s1.z).color(r, g, b, a).endVertex();
                buf.pos(s2.x, s2.y, s2.z).color(r, g, b, a).endVertex();
                buf.pos(e2.x, e2.y, e2.z).color(r, g, b, a).endVertex();
                buf.pos(e1.x, e1.y, e1.z).color(r, g, b, a).endVertex();
            }
        }

        tess.draw();
    }

    @Override
    public void render(ClientLaser laser, float partialTicks, double viewX, double viewY, double viewZ) {
        renderBatch(java.util.Collections.singleton(laser), partialTicks, viewX, viewY, viewZ);
    }
}
