package com.smd.bulletapi.client.render;

import com.smd.bulletapi.client.ClientLaser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Collection;

@SideOnly(Side.CLIENT)
public class LaserPolyRenderer implements ILaserRenderer {
    public static final LaserPolyRenderer INSTANCE = new LaserPolyRenderer();
    private LaserPolyRenderer() {}

    @Override
    public void beginRender() {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
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
        if (lasers.isEmpty()) return;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        double time = Minecraft.getMinecraft().world.getTotalWorldTime() + partialTicks;

        for (ClientLaser laser : lasers) {
            Vec3d start = laser.getRenderStart(partialTicks);
            Vec3d end = laser.getRenderEnd(partialTicks);
            Vec3d dir = end.subtract(start);
            double len = dir.length();
            if (len < 1.0E-6) continue;
            dir = dir.normalize();

            NBTTagCompound data = laser.getCustomData();
            int sides = data != null && data.hasKey("poly_sides") ? data.getInteger("poly_sides") : 6;
            sides = Math.max(3, Math.min(16, sides));
            float alpha = data != null && data.hasKey("alpha") ? data.getFloat("alpha") : 0.85f;
            float coreScale = data != null && data.hasKey("core_scale") ? data.getFloat("core_scale") : 0.45f;
            float shellScale = data != null && data.hasKey("shell_scale") ? data.getFloat("shell_scale") : 1.0f;
            float pulseAmp = data != null && data.hasKey("pulse_amp") ? data.getFloat("pulse_amp") : 0.15f;
            float pulseSpeed = data != null && data.hasKey("pulse_speed") ? data.getFloat("pulse_speed") : 0.35f;

            int coreColor = data != null && data.hasKey("core_color") ? data.getInteger("core_color") : 0xFFFFFF;
            int shellColor = data != null && data.hasKey("shell_color") ? data.getInteger("shell_color") : laser.getColor();
            int shellEnd = data != null && data.hasKey("shell_color_end") ? data.getInteger("shell_color_end") : shellColor;

            Vec3d viewDir = new Vec3d(viewX - start.x, viewY - start.y, viewZ - start.z);
            if (viewDir.lengthSquared() < 1.0E-6) viewDir = new Vec3d(0, 1, 0);
            viewDir = viewDir.normalize();

            Vec3d right = dir.crossProduct(viewDir);
            if (right.lengthSquared() < 1.0E-6) {
                right = dir.crossProduct(new Vec3d(0, 1, 0));
            }
            if (right.lengthSquared() < 1.0E-6) {
                right = new Vec3d(1, 0, 0);
            }
            right = right.normalize();
            Vec3d up = dir.crossProduct(right).normalize();

            float pulse = 1.0f + pulseAmp * (float) Math.sin(time * pulseSpeed);

            Vec3d s = new Vec3d(start.x - viewX, start.y - viewY, start.z - viewZ);
            Vec3d e = new Vec3d(end.x - viewX, end.y - viewY, end.z - viewZ);

            emitPoly(buf, s, e, right, up, laser.getThickness() * 0.5 * shellScale * pulse, sides,
                    shellColor, shellEnd, alpha * 0.8f);
            emitPoly(buf, s, e, right, up, laser.getThickness() * 0.5 * coreScale * pulse, sides,
                    coreColor, coreColor, alpha);
        }

        tess.draw();
    }

    @Override
    public void render(ClientLaser laser, float partialTicks, double viewX, double viewY, double viewZ) {
        renderBatch(java.util.Collections.singleton(laser), partialTicks, viewX, viewY, viewZ);
    }

    private static void emitPoly(BufferBuilder buf, Vec3d s, Vec3d e, Vec3d right, Vec3d up,
                                 double radius, int sides, int colorStart, int colorEnd, float alpha) {
        double step = (Math.PI * 2.0) / sides;
        for (int i = 0; i < sides; i++) {
            double a0 = step * i;
            double a1 = step * (i + 1);
            Vec3d o0 = right.scale(Math.cos(a0) * radius).add(up.scale(Math.sin(a0) * radius));
            Vec3d o1 = right.scale(Math.cos(a1) * radius).add(up.scale(Math.sin(a1) * radius));

            Vec3d s1 = s.add(o0);
            Vec3d s2 = s.add(o1);
            Vec3d e1 = e.add(o0);
            Vec3d e2 = e.add(o1);

            LaserRenderHelper.emitQuad(buf, s1, s2, e2, e1, colorStart, colorEnd, alpha);
        }
    }
}
