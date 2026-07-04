package com.smd.bulletapi.client;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.common.data.DataPayload;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;

@SideOnly(Side.CLIENT)
@InternalApi
public class ClientSummonCache extends AbstractClientBulletCache {
    public static ClientSummonCache INSTANCE;

    public ClientSummonCache() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void spawnSummon(int id, double positionX, double positionY, double positionZ,
                            double velocityX, double velocityY, double velocityZ,
                            int life, float damage,
                            ResourceLocation texture, int color, float size, String rendererType,
                            DataPayload customData) {
        spawnEntry(id, positionX, positionY, positionZ, velocityX, velocityY, velocityZ,
                life, damage, texture, color, size, rendererType, customData);
    }

    @Override
    protected ClientBullet createEntry(int id, double positionX, double positionY, double positionZ,
                                       double velocityX, double velocityY, double velocityZ,
                                       int life, float damage,
                                       ResourceLocation texture, int color, float size, String rendererType,
                                       DataPayload customData) {
        return new ClientSummon(id, positionX, positionY, positionZ, velocityX, velocityY, velocityZ,
                life, damage, texture, color, size, rendererType, customData);
    }

    public void updateSummon(int id, int flags,
                             double positionX, double positionY, double positionZ,
                             double velocityX, double velocityY, double velocityZ,
                             Integer life) {
        applyEntryUpdate(id, flags, positionX, positionY, positionZ, velocityX, velocityY, velocityZ, life);
    }

    public void updateVisual(int id, int flags, ResourceLocation texture, String rendererType, String renderState) {
        applyVisualUpdate(id, flags, texture, rendererType, renderState);
    }

    public void removeSummon(int id) {
        removeEntry(id);
    }

    public Map<Integer, ClientBullet> getSummons() {
        return getEntries();
    }
}
