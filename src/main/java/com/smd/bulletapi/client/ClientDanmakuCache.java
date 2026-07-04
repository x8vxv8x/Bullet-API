package com.smd.bulletapi.client;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.common.data.DataPayload;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;

@SideOnly(Side.CLIENT)
@InternalApi
public class ClientDanmakuCache extends AbstractClientBulletCache {
    public static ClientDanmakuCache INSTANCE;

    public ClientDanmakuCache() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    public void spawnBullet(int id, double positionX, double positionY, double positionZ,
                            double velocityX, double velocityY, double velocityZ,
                            int maxLife, float damage,
                            ResourceLocation texture, int color, float size, String rendererType,
                            DataPayload customData) {
        spawnEntry(id, positionX, positionY, positionZ, velocityX, velocityY, velocityZ,
                maxLife, damage, texture, color, size, rendererType, customData);
    }

    public void updateBullet(int id, int flags,
                             double positionX, double positionY, double positionZ,
                             double velocityX, double velocityY, double velocityZ,
                             Integer life) {
        applyEntryUpdate(id, flags, positionX, positionY, positionZ, velocityX, velocityY, velocityZ, life);
    }

    public void updateVisual(int id, int flags, ResourceLocation texture, String rendererType, String renderState) {
        applyVisualUpdate(id, flags, texture, rendererType, renderState);
    }

    public void removeBullet(int id) {
        removeEntry(id);
    }

    public Map<Integer, ClientBullet> getBullets() {
        return getEntries();
    }
}
