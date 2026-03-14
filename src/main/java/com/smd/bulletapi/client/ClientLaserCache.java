package com.smd.bulletapi.client;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.client.render.LaserBeamRenderer;
import com.smd.bulletapi.client.render.LaserRendererRegistry;
import com.smd.bulletapi.client.render.ILaserRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
@InternalApi
public class ClientLaserCache {
    public static ClientLaserCache INSTANCE;

    private final Map<Integer, ClientLaser> lasers = new HashMap<>();

    public ClientLaserCache() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void spawnLaser(int id, long tick, Vec3d start, Vec3d direction, double length,
                           float thickness, int color, String rendererType,
                           NBTTagCompound customData) {
        ClientLaser laser = new ClientLaser(id, tick, start, direction, length, thickness, color, rendererType, customData);

        if (rendererType != null && LaserRendererRegistry.hasType(rendererType)) {
            laser.setRenderer(LaserRendererRegistry.create(rendererType, customData));
        } else {
            laser.setRenderer(LaserBeamRenderer.INSTANCE);
        }

        lasers.put(id, laser);
    }

    public void updateLaser(int id, long tick, Vec3d start, Vec3d direction, double length) {
        ClientLaser laser = lasers.get(id);
        if (laser != null) {
            laser.update(tick, start, direction, length);
        }
    }

    public void removeLaser(int id) {
        ClientLaser laser = lasers.remove(id);
        if (laser != null && laser.getRenderer() != null) {
            laser.getRenderer().deleteGlResources();
        }
    }

    public void clear() {
        for (ClientLaser laser : lasers.values()) {
            ILaserRenderer renderer = laser.getRenderer();
            if (renderer != null) {
                renderer.deleteGlResources();
            }
        }
        lasers.clear();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null || mc.isGamePaused()) return;

        for (ClientLaser laser : lasers.values()) {
            laser.tick();
        }
    }

    @SubscribeEvent
    public void onClientWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            clear();
        }
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        clear();
    }

    public Map<Integer, ClientLaser> getLasers() {
        return lasers;
    }
}
