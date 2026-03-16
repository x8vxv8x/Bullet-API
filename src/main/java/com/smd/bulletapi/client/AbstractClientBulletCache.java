package com.smd.bulletapi.client;

import com.smd.bulletapi.api.annotation.InternalApi;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
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
abstract class AbstractClientBulletCache {
    private final Map<Integer, ClientBullet> entries = new HashMap<>();

    protected final void spawnEntry(int id, Vec3d position, Vec3d velocity, int life, float damage,
                                    ResourceLocation texture, int color, float size, String rendererType,
                                    NBTTagCompound customData) {
        ClientBullet entry = createEntry(id, position, velocity, life, damage,
                texture, color, size, rendererType, customData);
        entry.setRenderer(ClientRendererResolvers.createBulletRenderer(texture, rendererType, customData));
        entries.put(id, entry);
    }

    protected ClientBullet createEntry(int id, Vec3d position, Vec3d velocity, int life, float damage,
                                       ResourceLocation texture, int color, float size, String rendererType,
                                       NBTTagCompound customData) {
        return new ClientBullet(id, position, velocity, life, damage, texture, color, size, rendererType, customData);
    }

    protected final void updateEntryVelocity(int id, Vec3d velocity) {
        ClientBullet entry = entries.get(id);
        if (entry != null) {
            entry.setVelocity(velocity);
        }
    }

    protected final void applyEntrySnapshot(int id, Vec3d position, Vec3d velocity, int life) {
        ClientBullet entry = entries.get(id);
        if (entry != null) {
            entry.applySnapshot(position, velocity, life);
        }
    }

    protected final void applyEntryUpdate(int id, Vec3d position, Vec3d velocity, Integer life) {
        ClientBullet entry = entries.get(id);
        if (entry != null) {
            entry.applyUpdate(position, velocity, life);
        }
    }

    protected final void removeEntry(int id) {
        ClientBullet entry = entries.remove(id);
        if (entry != null) {
            ClientRendererResolvers.deleteBulletRenderer(entry.getRenderer());
        }
    }

    public final void clear() {
        for (ClientBullet entry : entries.values()) {
            ClientRendererResolvers.deleteBulletRenderer(entry.getRenderer());
        }
        entries.clear();
    }

    @SubscribeEvent
    public final void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null || mc.isGamePaused()) return;

        for (ClientBullet entry : entries.values()) {
            entry.tick();
        }
        entries.entrySet().removeIf(entry -> {
            if (!entry.getValue().isDead()) {
                return false;
            }
            ClientRendererResolvers.deleteBulletRenderer(entry.getValue().getRenderer());
            return true;
        });
    }

    @SubscribeEvent
    public final void onClientWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            clear();
        }
    }

    @SubscribeEvent
    public final void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        clear();
    }

    protected final Map<Integer, ClientBullet> getEntries() {
        return entries;
    }
}
