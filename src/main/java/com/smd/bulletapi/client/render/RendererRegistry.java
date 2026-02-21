package com.smd.bulletapi.client.render;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@SideOnly(Side.CLIENT)
public class RendererRegistry {
    private static final Map<String, Function<NBTTagCompound, IBulletRenderer>> REGISTRY = new HashMap<>();

    /**
     * 注册一个渲染器工厂
     * @param type    渲染器类型标识符（必须唯一）
     * @param factory 接收 customData 并返回 IBulletRenderer 实例的函数
     */
    public static void register(String type, Function<NBTTagCompound, IBulletRenderer> factory) {
        REGISTRY.put(type, factory);
    }

    /**
     * 根据类型和自定义数据创建渲染器实例
     * @param type 渲染器类型
     * @param data 自定义数据（可能包含纹理路径、颜色等）
     * @return 渲染器实例，若类型未注册则返回 null
     */
    public static IBulletRenderer create(String type, NBTTagCompound data) {
        Function<NBTTagCompound, IBulletRenderer> factory = REGISTRY.get(type);
        return factory != null ? factory.apply(data) : null;
    }

    /**
     * 判断类型是否已注册
     */
    public static boolean hasType(String type) {
        return REGISTRY.containsKey(type);
    }
}