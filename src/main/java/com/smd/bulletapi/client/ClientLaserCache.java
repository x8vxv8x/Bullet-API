package com.smd.bulletapi.client;

import com.smd.bulletapi.client.render.LaserBeamRenderer;
import com.smd.bulletapi.client.render.LaserRendererRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SideOnly(Side.CLIENT)
public class ClientLaserCache {
    public static ClientLaserCache INSTANCE;

    private final Map<Integer, ClientLaser> lasers = new ConcurrentHashMap<>();

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

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (ClientLaser laser : lasers.values()) {
                laser.tick();
            }
        }
    }

    public Map<Integer, ClientLaser> getLasers() {
        return lasers;
    }
}
