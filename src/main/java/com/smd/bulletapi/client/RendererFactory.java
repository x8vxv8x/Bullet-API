package com.smd.bulletapi.client;

import com.smd.bulletapi.client.render.BillboardRenderer;
import com.smd.bulletapi.client.render.IBulletRenderer;
import com.smd.bulletapi.client.render.PointSpriteRenderer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class RendererFactory {
    private static final Map<String, Function<CreationContext, IBulletRenderer>> REGISTRY = new HashMap<>();

    static {
        // 注册内置渲染器
        REGISTRY.put("billboard", ctx -> BillboardRenderer.INSTANCE);
        REGISTRY.put("point", ctx -> PointSpriteRenderer.INSTANCE);
        // 可在此扩展其他渲染器，如 "entity_model"
    }

    /**
     * 根据类型、纹理和自定义数据创建渲染器
     * @param type       渲染器类型标识（可为null）
     * @param texture    纹理路径（可为null）
     * @param customData 自定义数据
     * @return 渲染器实例，如果无法创建则返回默认渲染器（公告板或点精灵）
     */
    public static IBulletRenderer create(String type, ResourceLocation texture, NBTTagCompound customData) {
        CreationContext ctx = new CreationContext(texture, customData);
        if (type != null && REGISTRY.containsKey(type)) {
            return REGISTRY.get(type).apply(ctx);
        }
        // 自动选择：有纹理用公告板，否则点精灵
        return texture != null ? BillboardRenderer.INSTANCE : PointSpriteRenderer.INSTANCE;
    }

    public static void register(String type, Function<CreationContext, IBulletRenderer> factory) {
        REGISTRY.put(type, factory);
    }

    public static class CreationContext {
        public final ResourceLocation texture;
        public final NBTTagCompound customData;
        public CreationContext(ResourceLocation texture, NBTTagCompound customData) {
            this.texture = texture;
            this.customData = customData;
        }
    }
}
