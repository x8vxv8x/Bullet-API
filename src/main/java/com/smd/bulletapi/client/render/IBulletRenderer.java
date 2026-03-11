package com.smd.bulletapi.client.render;

import com.smd.bulletapi.client.ClientBullet;

import java.util.Collection;

public interface IBulletRenderer {

    /**
     * 每个渲染器分组开始渲染前调用，可覆盖默认渲染状态
     */
    default void beginRender() {}

    /**
     * 每个渲染器分组渲染结束后调用，用于恢复渲染状态
     */
    default void endRender() {}

    default boolean canBatch() { return false; }

    default void renderBatch(Collection<ClientBullet> bullets, float partialTicks, double viewX, double viewY, double viewZ) {
        // 不支持批量的渲染器可逐个渲染（回退）
        for (ClientBullet bullet : bullets) {
            render(bullet, partialTicks, viewX, viewY, viewZ);
        }
    }
    /**
     * 渲染该弹幕
     * @param bullet      弹幕数据
     * @param partialTicks 渲染帧部分 ticks
     * @param viewX, viewY, viewZ 摄像机位置（用于坐标变换）
     */
    void render(ClientBullet bullet, float partialTicks, double viewX, double viewY, double viewZ);

    /**
     * 当弹幕被移除或世界卸载时调用，用于释放 OpenGL 资源
     */
    default void deleteGlResources() {}
}
