package com.smd.bulletapi.client.render;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@SideOnly(Side.CLIENT)
public class LaserRendererRegistry {
    private static final Map<String, Function<NBTTagCompound, ILaserRenderer>> REGISTRY = new HashMap<>();

    public static void register(String type, Function<NBTTagCompound, ILaserRenderer> factory) {
        REGISTRY.put(type, factory);
    }

    public static ILaserRenderer create(String type, NBTTagCompound data) {
        Function<NBTTagCompound, ILaserRenderer> factory = REGISTRY.get(type);
        return factory != null ? factory.apply(data) : null;
    }

    public static boolean hasType(String type) {
        return REGISTRY.containsKey(type);
    }
}
