package com.smd.bulletapi.client;

import com.smd.bulletapi.api.annotation.InternalApi;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
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

    public void spawnBullet(int id, Vec3d position, Vec3d velocity, int maxLife, float damage,
                            ResourceLocation texture, int color, float size, String rendererType,
                            NBTTagCompound customData) {
        spawnEntry(id, position, velocity, maxLife, damage, texture, color, size, rendererType, customData);
    }

    public void updateBulletVelocity(int id, Vec3d velocity) {
        updateEntryVelocity(id, velocity);
    }

    public void removeBullet(int id) {
        removeEntry(id);
    }

    public Map<Integer, ClientBullet> getBullets() {
        return getEntries();
    }
}
