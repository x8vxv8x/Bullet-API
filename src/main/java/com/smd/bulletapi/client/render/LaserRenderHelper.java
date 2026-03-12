package com.smd.bulletapi.client.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.Vec3d;

public final class LaserRenderHelper {
    private LaserRenderHelper() {}

    public static int colorLerp(float t, int colorA, int colorB) {
        int rA = (colorA >> 16) & 0xFF;
        int gA = (colorA >> 8) & 0xFF;
        int bA = colorA & 0xFF;
        int rB = (colorB >> 16) & 0xFF;
        int gB = (colorB >> 8) & 0xFF;
        int bB = colorB & 0xFF;
        int r = (int) (rA + (rB - rA) * t);
        int g = (int) (gA + (gB - gA) * t);
        int b = (int) (bA + (bB - bA) * t);
        return (r << 16) | (g << 8) | b;
    }

    public static int color255(int r, int g, int b) {
        return (r << 16) | (g << 8) | b;
    }

    public static void emitQuad(BufferBuilder buf, Vec3d s1, Vec3d s2, Vec3d e2, Vec3d e1,
                                int colorStart, int colorEnd, float alpha) {
        float[] c0 = colorToRgb(colorStart);
        float[] c1 = colorToRgb(colorEnd);

        buf.pos(s1.x, s1.y, s1.z).color(c0[0], c0[1], c0[2], alpha).endVertex();
        buf.pos(s2.x, s2.y, s2.z).color(c0[0], c0[1], c0[2], alpha).endVertex();
        buf.pos(e2.x, e2.y, e2.z).color(c1[0], c1[1], c1[2], alpha).endVertex();
        buf.pos(e1.x, e1.y, e1.z).color(c1[0], c1[1], c1[2], alpha).endVertex();
    }

    private static float[] colorToRgb(int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        return new float[]{r, g, b};
    }
}
