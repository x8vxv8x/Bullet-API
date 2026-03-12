package com.smd.bulletapi.network;


import com.smd.bulletapi.Tags;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);
    private static int id = 0;

    public static void registerMessages() {
        INSTANCE.registerMessage(SPacketDanmaku.Handler.class, SPacketDanmaku.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(SPacketLaser.Handler.class, SPacketLaser.class, id++, Side.CLIENT);
    }

    public static void sendToAll(IMessage message) {
        INSTANCE.sendToAll(message);
    }

    public static void sendToDimension(IMessage message, int dimension) {
        INSTANCE.sendToDimension(message, dimension);
    }
}
