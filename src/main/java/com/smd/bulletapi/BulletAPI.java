package com.smd.bulletapi;

import com.smd.bulletapi.common.DanmakuManager;
import com.smd.bulletapi.common.summon.SummonManager;
import com.smd.bulletapi.common.summon.SummonRegistry;
import com.smd.bulletapi.network.PacketHandler;
import com.smd.bulletapi.proxy.CommonProxy;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class BulletAPI {

    @SidedProxy(clientSide = "com.smd.bulletapi.proxy.ClientProxy",
                serverSide = "com.smd.bulletapi.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        PacketHandler.registerMessages();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(DanmakuManager.getInstance());
        MinecraftForge.EVENT_BUS.register(SummonManager.getInstance());
        MinecraftForge.EVENT_BUS.register(SummonManager.getInstance().getSlotManager());
        SummonRegistry.bootstrapDefaults();
        proxy.init(event);
    }
}
