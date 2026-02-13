package com.smd.bulletapi.proxy;

import com.smd.bulletapi.client.ClientDanmakuCache;
import com.smd.bulletapi.client.RenderHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ClientDanmakuCache.INSTANCE = new ClientDanmakuCache(); // 注册客户端事件
        MinecraftForge.EVENT_BUS.register(RenderHandler.class);
    }
}
