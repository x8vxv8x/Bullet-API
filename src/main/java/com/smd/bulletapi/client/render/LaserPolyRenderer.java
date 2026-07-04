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
            int sides = data != null && data.hasKey("poly_sides") ? data.getInteger("poly_sides") : 6;
            sides = Math.max(3, Math.min(16, sides));
            float alpha = data != null && data.hasKey("alpha") ? data.getFloat("alpha") : 0.85f;
            float coreScale = data != null && data.hasKey("core_scale") ? data.getFloat("core_scale") : 0.45f;
            float shellScale = data != null && data.hasKey("shell_scale") ? data.getFloat("shell_scale") : 1.0f;
            float pulseAmp = data != null && data.hasKey("pulse_amp") ? data.getFloat("pulse_amp") : 0.15f;
            float pulseSpeed = data != null && data.hasKey("pulse_speed") ? data.getFloat("pulse_speed") : 0.35f;
            float twistSpeed = data != null && data.hasKey("twist_speed") ? data.getFloat("twist_speed") : 0.0f;
            float twistStep = data != null && data.hasKey("twist_step") ? data.getFloat("twist_step") : 0.0f;
            float jitterAmp = data != null && data.hasKey("jitter_amp") ? data.getFloat("jitter_amp") : 0.0f;
            float jitterFreq = data != null && data.hasKey("jitter_freq") ? data.getFloat("jitter_freq") : 0.6f;
            int coreColor = data != null && data.hasKey("core_color") ? data.getInteger("core_color") : 0xFFFFFF;
            int shellColor = data != null && data.hasKey("shell_color") ? data.getInteger("shell_color") : laser.getColor();
            int shellEnd = data != null && data.hasKey("shell_color_end") ? data.getInteger("shell_color_end") : shellColor;

            boolean decoOn = data != null && data.hasKey("deco_on") && data.getBoolean("deco_on");
            float decoScale = data != null && data.hasKey("deco_scale") ? data.getFloat("deco_scale") : 1.3f;
            float decoAlpha = data != null && data.hasKey("deco_alpha") ? data.getFloat("deco_alpha") : 0.55f;
            float decoStep = data != null && data.hasKey("deco_step") ? data.getFloat("deco_step") : 1.4f;
            float decoScroll = data != null && data.hasKey("deco_scroll") ? data.getFloat("deco_scroll") : 0.6f;
            float decoRotSpeed = data != null && data.hasKey("deco_rot_speed") ? data.getFloat("deco_rot_speed") : 0.6f;
            int decoColor = data != null && data.hasKey("deco_color") ? data.getInteger("deco_color") : shellColor;

            boolean useBlock = data != null && data.hasKey("block_len") && data.hasKey("block_color_a") && data.hasKey("block_color_b");
            float blockLen = useBlock ? Math.max(0.1f, data.getFloat("block_len")) : 0.0f;
            float blockSpeed = useBlock && data.hasKey("block_speed") ? data.getFloat("block_speed") : 0.0f;
            boolean blockSoft = useBlock && data.hasKey("block_soft") && data.getBoolean("block_soft");
            boolean blockCore = useBlock && data.hasKey("block_apply_core") && data.getBoolean("block_apply_core");
            int blockColorA = useBlock ? data.getInteger("block_color_a") : shellColor;
            int blockColorB = useBlock ? data.getInteger("block_color_b") : shellColor;

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
            Vec3d up = dir.crossProduct(right).normalize();

            float pulse = 1.0f + pulseAmp * (float) Math.sin(time * pulseSpeed);

            int segments = useBlock ? Math.max(1, (int) Math.ceil(len / blockLen)) : 1;
            double step = len / segments;

            for (int i = 0; i < segments; i++) {
                double t0 = step * i;
                double t1 = step * (i + 1);
                Vec3d sWorld = start.add(dir.scale(t0));
                Vec3d eWorld = start.add(dir.scale(t1));
                double mid = (t0 + t1) * 0.5;

                Vec3d localRight = right;
                Vec3d localUp = up;
                if (twistSpeed != 0.0f || twistStep != 0.0f) {
                    double theta = time * twistSpeed + i * twistStep;
                    localRight = rotateAroundAxis(right, up, theta, true);
                    localUp = rotateAroundAxis(right, up, theta, false);
                }

                if (jitterAmp > 0.0f) {
                    double jx = Math.sin(time * jitterFreq + i * 0.77);
                    double jy = Math.cos(time * jitterFreq + i * 0.31);
                    Vec3d jitter = localRight.scale(jx * jitterAmp).add(localUp.scale(jy * jitterAmp));
                    sWorld = sWorld.add(jitter);
                    eWorld = eWorld.add(jitter);
                }

                Vec3d segS = new Vec3d(sWorld.x - viewX, sWorld.y - viewY, sWorld.z - viewZ);
                Vec3d segE = new Vec3d(eWorld.x - viewX, eWorld.y - viewY, eWorld.z - viewZ);

                int segShellStart = shellColor;
                int segShellEnd = shellEnd;
                int segCoreColor = coreColor;

                if (useBlock) {
                    segShellStart = blockColorAt(t0 + time * blockSpeed, blockLen, blockColorA, blockColorB, blockSoft);
                    segShellEnd = blockColorAt(t1 + time * blockSpeed, blockLen, blockColorA, blockColorB, blockSoft);
                    if (blockCore) {
                        segCoreColor = blockColorAt((t0 + t1) * 0.5 + time * blockSpeed, blockLen, blockColorA, blockColorB, blockSoft);
                    }
                }

                emitPoly(buf, segS, segE, localRight, localUp, laser.getThickness() * 0.5 * shellScale * pulse, sides,
                        segShellStart, segShellEnd, alpha * 0.8f);
                emitPoly(buf, segS, segE, localRight, localUp, laser.getThickness() * 0.5 * coreScale * pulse, sides,
                        segCoreColor, segCoreColor, alpha);

                if (decoOn) {
                    double decoPos = mid + (time * decoScroll);
                    double decoOffset = decoStep <= 0.0f ? 0.0f : (decoPos % decoStep) - (decoStep * 0.5);
                    Vec3d dCenter = start.add(dir.scale(decoPos - decoOffset));
                    Vec3d ds = new Vec3d(dCenter.x - viewX, dCenter.y - viewY, dCenter.z - viewZ);
                    Vec3d de = ds.add(dir.scale(decoStep <= 0.0f ? step : decoStep));

                    double decoTheta = time * decoRotSpeed + i * 0.4;
                    Vec3d decoRight = rotateAroundAxis(localRight, localUp, decoTheta, true);
                    Vec3d decoUp = rotateAroundAxis(localRight, localUp, decoTheta, false);
                    emitSquareRing(buf, ds, de, decoRight, decoUp,
                            laser.getThickness() * 0.5 * shellScale * decoScale, decoColor, decoAlpha);
                }
            }
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

    private static int blockColorAt(double position, float blockLen, int colorA, int colorB, boolean soft) {
        if (blockLen <= 0.0f) {
            return colorA;
        }
        double unit = position / blockLen;
        if (!soft) {
            int idx = (int) Math.floor(unit);
            return (idx & 1) == 0 ? colorA : colorB;
        }
        double frac = unit - Math.floor(unit);
        return LaserRenderHelper.colorLerp((float) frac, colorA, colorB);
    }

    private static Vec3d rotateAroundAxis(Vec3d right, Vec3d up, double theta, boolean outRight) {
        double c = Math.cos(theta);
        double s = Math.sin(theta);
        if (outRight) {
            return right.scale(c).add(up.scale(s));
        }
        return up.scale(c).subtract(right.scale(s));
    }

    private static void emitSquareRing(BufferBuilder buf, Vec3d s, Vec3d e, Vec3d right, Vec3d up,
                                       double radius, int color, float alpha) {
        Vec3d o0 = right.scale(radius).add(up.scale(radius));
        Vec3d o1 = right.scale(-radius).add(up.scale(radius));
        Vec3d o2 = right.scale(-radius).add(up.scale(-radius));
        Vec3d o3 = right.scale(radius).add(up.scale(-radius));

        Vec3d s0 = s.add(o0);
        Vec3d s1 = s.add(o1);
        Vec3d s2 = s.add(o2);
        Vec3d s3 = s.add(o3);
        Vec3d e0 = e.add(o0);
        Vec3d e1 = e.add(o1);
        Vec3d e2 = e.add(o2);
        Vec3d e3 = e.add(o3);

        LaserRenderHelper.emitQuad(buf, s0, s1, e1, e0, color, color, alpha);
        LaserRenderHelper.emitQuad(buf, s1, s2, e2, e1, color, color, alpha);
        LaserRenderHelper.emitQuad(buf, s2, s3, e3, e2, color, color, alpha);
        LaserRenderHelper.emitQuad(buf, s3, s0, e0, e3, color, color, alpha);
    }
}
