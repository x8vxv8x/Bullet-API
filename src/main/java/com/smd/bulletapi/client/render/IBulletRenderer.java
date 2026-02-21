package com.smd.bulletapi.client.render;

import com.smd.bulletapi.client.ClientBullet;

public interface IBulletRenderer {
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
