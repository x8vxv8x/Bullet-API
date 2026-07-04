package com.smd.bulletapi.client;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.client.render.BillboardRenderer;
import com.smd.bulletapi.client.render.IBulletRenderer;
import com.smd.bulletapi.client.render.ILaserRenderer;
import com.smd.bulletapi.client.render.LaserBeamRenderer;
import com.smd.bulletapi.client.render.LaserRendererRegistry;
import com.smd.bulletapi.client.render.PointSpriteRenderer;
import com.smd.bulletapi.client.render.RendererRegistry;
import com.smd.bulletapi.common.data.DataPayload;
import net.minecraft.util.ResourceLocation;

@InternalApi
final class ClientRendererResolvers {
    private ClientRendererResolvers() {}

    static IBulletRenderer createBulletRenderer(ResourceLocation texture, String rendererType, DataPayload customData) {
        if (rendererType != null && RendererRegistry.hasType(rendererType)) {
            return RendererRegistry.create(rendererType, customData);
        }
        return texture != null ? BillboardRenderer.INSTANCE : PointSpriteRenderer.INSTANCE;
    }

    static ILaserRenderer createLaserRenderer(String rendererType, DataPayload customData) {
        if (rendererType != null && LaserRendererRegistry.hasType(rendererType)) {
            return LaserRendererRegistry.create(rendererType, customData);
        }
        return LaserBeamRenderer.INSTANCE;
    }

    static void deleteBulletRenderer(IBulletRenderer renderer) {
        if (renderer != null) {
            renderer.deleteGlResources();
        }
    }

    static void deleteLaserRenderer(ILaserRenderer renderer) {
        if (renderer != null) {
            renderer.deleteGlResources();
        }
    }
}
