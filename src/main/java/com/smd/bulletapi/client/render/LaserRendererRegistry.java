package com.smd.bulletapi.client.render;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.common.data.DataPayload;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@SideOnly(Side.CLIENT)
@PublicApi
public class LaserRendererRegistry {
    private static final Map<String, Function<DataPayload, ILaserRenderer>> REGISTRY = new HashMap<>();

    public static void register(String type, Function<DataPayload, ILaserRenderer> factory) {
        REGISTRY.put(type, factory);
    }

    public static ILaserRenderer create(String type, DataPayload data) {
        Function<DataPayload, ILaserRenderer> factory = REGISTRY.get(type);
        return factory != null ? factory.apply(data) : null;
    }

    public static boolean hasType(String type) {
        return REGISTRY.containsKey(type);
    }
}
