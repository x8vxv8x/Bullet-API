package com.smd.bulletapi.client;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.common.data.DataPayload;
import com.smd.bulletapi.network.SPacketBulletVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
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

    protected final void spawnEntry(int id, double positionX, double positionY, double positionZ,
                                    double velocityX, double velocityY, double velocityZ,
                                    int life, float damage,
                                    ResourceLocation texture, int color, float size, String rendererType,
                                    DataPayload customData) {
        ClientBullet entry = createEntry(id, positionX, positionY, positionZ, velocityX, velocityY, velocityZ, life, damage,
                texture, color, size, rendererType, customData);
        entry.setRenderer(ClientRendererResolvers.createBulletRenderer(texture, rendererType, customData));
        entries.put(id, entry);
    }

    protected ClientBullet createEntry(int id, double positionX, double positionY, double positionZ,
                                       double velocityX, double velocityY, double velocityZ,
                                       int life, float damage,
                                       ResourceLocation texture, int color, float size, String rendererType,
                                       DataPayload customData) {
        return new ClientBullet(id, positionX, positionY, positionZ, velocityX, velocityY, velocityZ,
                life, damage, texture, color, size, rendererType, customData);
    }

    protected final void applyEntryUpdate(int id, int flags,
                                          double positionX, double positionY, double positionZ,
                                          double velocityX, double velocityY, double velocityZ,
                                          Integer life) {
        ClientBullet entry = entries.get(id);
        if (entry != null) {
            entry.applyUpdate(
                    (flags & com.smd.bulletapi.network.SPacketDanmaku.FLAG_POSITION) != 0,
                    positionX,
                    positionY,
                    positionZ,
                    (flags & com.smd.bulletapi.network.SPacketDanmaku.FLAG_VELOCITY) != 0,
                    velocityX,
                    velocityY,
                    velocityZ,
                    life
            );
        }
    }

    protected final void applyVisualUpdate(int id, int flags, ResourceLocation texture, String rendererType, String renderState) {
        ClientBullet entry = entries.get(id);
        if (entry == null) {
            return;
        }

        if ((flags & SPacketBulletVisual.FLAG_RENDER_STATE) != 0) {
            entry.setRenderState(renderState);
        }

        boolean rebuildRenderer = false;
        if ((flags & SPacketBulletVisual.FLAG_TEXTURE) != 0) {
            entry.setTexture(texture);
            rebuildRenderer = true;
        }
        if ((flags & SPacketBulletVisual.FLAG_RENDERER) != 0) {
            entry.setRendererType(rendererType);
            rebuildRenderer = true;
        }

        if (rebuildRenderer) {
            ClientRendererResolvers.deleteBulletRenderer(entry.getRenderer());
            entry.setRenderer(ClientRendererResolvers.createBulletRenderer(entry.getTexture(), entry.getRendererType(), entry.getCustomData()));
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
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null || mc.isGamePaused()) {
            return;
        }

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
