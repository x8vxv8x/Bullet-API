package com.smd.bulletapi.client;

import com.smd.bulletapi.api.annotation.InternalApi;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
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

    public void spawnSummon(int id, Vec3d position, Vec3d velocity, int life, float damage,
                            ResourceLocation texture, int color, float size, String rendererType,
                            NBTTagCompound customData) {
        spawnEntry(id, position, velocity, life, damage, texture, color, size, rendererType, customData);
    }

    public void updateSummon(int id, Vec3d position, Vec3d velocity, int life) {
        applyEntrySnapshot(id, position, velocity, life);
    }

    public void removeSummon(int id) {
        removeEntry(id);
    }

    public Map<Integer, ClientBullet> getSummons() {
        return getEntries();
    }
}
