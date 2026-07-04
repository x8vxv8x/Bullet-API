package com.smd.bulletapi.client.render;

import com.smd.bulletapi.client.ClientLaser;
import com.smd.bulletapi.common.data.DataPayload;
import net.minecraft.client.Minecraft;
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
public class LaserBlastRenderer implements ILaserRenderer {
    public static final LaserBlastRenderer INSTANCE = new LaserBlastRenderer();
    private LaserBlastRenderer() {}

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

        double time = Minecraft.getMinecraft().world.getTotalWorldTime() + partialTicks;

        for (ClientLaser laser : lasers) {
            Vec3d start = laser.getRenderStart(partialTicks);
            Vec3d end = laser.getRenderEnd(partialTicks);
            Vec3d dir = end.subtract(start);
            double len = dir.length();
            if (len < 1.0E-6) {
                continue;
            }
            dir = dir.normalize();

            DataPayload data = laser.getCustomData();
            float alpha = data != null && data.hasKey("alpha") ? data.getFloat("alpha") : 0.85f;
            float segmentLen = data != null && data.hasKey("segment_len") ? data.getFloat("segment_len") : 1.0f;
            float coreScale = data != null && data.hasKey("core_scale") ? data.getFloat("core_scale") : 0.55f;
            float shellScale = data != null && data.hasKey("shell_scale") ? data.getFloat("shell_scale") : 1.0f;
            float pulseAmp = data != null && data.hasKey("pulse_amp") ? data.getFloat("pulse_amp") : 0.2f;
            float pulseSpeed = data != null && data.hasKey("pulse_speed") ? data.getFloat("pulse_speed") : 0.35f;

            int coreColor = data != null && data.hasKey("core_color") ? data.getInteger("core_color") : 0xFFFFFF;
            int shellColor = data != null && data.hasKey("shell_color") ? data.getInteger("shell_color") : laser.getColor();
            int shellEnd = data != null && data.hasKey("shell_color_end") ? data.getInteger("shell_color_end") : shellColor;

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
            right = right.normalize();

            int segments = Math.max(1, (int) Math.ceil(len / Math.max(0.1, segmentLen)));
            double step = len / segments;
            for (int i = 0; i < segments; i++) {
                double t0 = step * i;
                double t1 = step * (i + 1);
                Vec3d sWorld = start.add(dir.scale(t0));
                Vec3d eWorld = start.add(dir.scale(t1));

                float phase = (float) (time * pulseSpeed + i * 0.35);
                float pulse = 1.0f + pulseAmp * (float) Math.sin(phase);

                Vec3d s = new Vec3d(sWorld.x - viewX, sWorld.y - viewY, sWorld.z - viewZ);
                Vec3d e = new Vec3d(eWorld.x - viewX, eWorld.y - viewY, eWorld.z - viewZ);

                Vec3d shellRight = right.scale(laser.getThickness() * 0.5 * shellScale * pulse);
                Vec3d coreRight = right.scale(laser.getThickness() * 0.5 * coreScale * pulse);

                Vec3d ss1 = s.add(shellRight);
                Vec3d ss2 = s.subtract(shellRight);
                Vec3d se1 = e.add(shellRight);
                Vec3d se2 = e.subtract(shellRight);
                LaserRenderHelper.emitQuad(buf, ss1, ss2, se2, se1, shellColor, shellEnd, alpha * 0.85f);

                Vec3d cs1 = s.add(coreRight);
                Vec3d cs2 = s.subtract(coreRight);
                Vec3d ce1 = e.add(coreRight);
                Vec3d ce2 = e.subtract(coreRight);
                LaserRenderHelper.emitQuad(buf, cs1, cs2, ce2, ce1, coreColor, coreColor, alpha);
            }
        }

        tess.draw();
    }

    @Override
    public void render(ClientLaser laser, float partialTicks, double viewX, double viewY, double viewZ) {
        renderBatch(java.util.Collections.singleton(laser), partialTicks, viewX, viewY, viewZ);
    }
}
