package com.smd.bulletapi.client.render;

import com.smd.bulletapi.client.ClientBullet;

import java.util.Collection;

public interface IBulletRenderer {

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
