package com.smd.bulletapi.proxy;

import com.smd.bulletapi.client.ClientDanmakuCache;
import com.smd.bulletapi.client.ClientLaserCache;
import com.smd.bulletapi.client.ClientSummonCache;
import com.smd.bulletapi.client.RenderHandler;
import com.smd.bulletapi.client.render.BillboardRenderer;
import com.smd.bulletapi.client.render.LaserBeamRenderer;
import com.smd.bulletapi.client.render.LaserBlastRenderer;
import com.smd.bulletapi.client.render.LaserRendererRegistry;
import com.smd.bulletapi.client.render.LaserNoneRenderer;
import com.smd.bulletapi.client.render.LaserPolyRenderer;
import com.smd.bulletapi.client.render.ModelJsonRenderer;
import com.smd.bulletapi.client.render.PointSpriteRenderer;
import com.smd.bulletapi.client.render.RendererRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        RendererRegistry.register("billboard", data -> BillboardRenderer.INSTANCE);
        RendererRegistry.register("point", data -> PointSpriteRenderer.INSTANCE);
        RendererRegistry.register("model_json", data -> ModelJsonRenderer.INSTANCE);
        LaserRendererRegistry.register("laser_beam", data -> LaserBeamRenderer.INSTANCE);
        LaserRendererRegistry.register("laser_blast", data -> LaserBlastRenderer.INSTANCE);
        LaserRendererRegistry.register("laser_none", data -> LaserNoneRenderer.INSTANCE);
        LaserRendererRegistry.register("laser_poly", data -> LaserPolyRenderer.INSTANCE);

        ClientDanmakuCache.INSTANCE = new ClientDanmakuCache(); // 注册客户端事件
        ClientLaserCache.INSTANCE = new ClientLaserCache();
        ClientSummonCache.INSTANCE = new ClientSummonCache();
        MinecraftForge.EVENT_BUS.register(RenderHandler.class);
    }
}
