package com.smd.bulletapi.client;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.client.render.BillboardRenderer;
import com.smd.bulletapi.client.render.IBulletRenderer;
import com.smd.bulletapi.client.render.PointSpriteRenderer;
import com.smd.bulletapi.client.render.RendererRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SideOnly(Side.CLIENT)
@InternalApi
public class ClientSummonCache {
    public static ClientSummonCache INSTANCE;

    private final Map<Integer, ClientBullet> summons = new ConcurrentHashMap<>();

    public ClientSummonCache() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void spawnSummon(int id, Vec3d position, Vec3d velocity, int life, float damage,
                            ResourceLocation texture, int color, float size, String rendererType,
                            NBTTagCompound customData) {
        ClientBullet summon = new ClientBullet(id, position, velocity, life, damage, texture, color, size, rendererType, customData);
        if (rendererType != null && RendererRegistry.hasType(rendererType)) {
            summon.setRenderer(RendererRegistry.create(rendererType, customData));
        } else {
            summon.setRenderer(texture != null ? BillboardRenderer.INSTANCE : PointSpriteRenderer.INSTANCE);
        }
        summons.put(id, summon);
    }

    public void updateSummon(int id, Vec3d position, Vec3d velocity, int life) {
        ClientBullet summon = summons.get(id);
        if (summon != null) {
            summon.applySnapshot(position, velocity, life);
        }
    }

    public void removeSummon(int id) {
        ClientBullet summon = summons.remove(id);
        if (summon == null) return;
        IBulletRenderer renderer = summon.getRenderer();
        if (renderer != null) {
            renderer.deleteGlResources();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        summons.values().forEach(ClientBullet::tick);
        summons.entrySet().removeIf(entry -> {
            if (!entry.getValue().isDead()) return false;
            IBulletRenderer renderer = entry.getValue().getRenderer();
            if (renderer != null) {
                renderer.deleteGlResources();
            }
            return true;
        });
    }

    public Map<Integer, ClientBullet> getSummons() {
        return summons;
    }
}
