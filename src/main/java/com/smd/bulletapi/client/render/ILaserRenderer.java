package com.smd.bulletapi.client.render;

import com.smd.bulletapi.api.annotation.SpiApi;
import com.smd.bulletapi.client.ClientLaser;

import java.util.Collection;

@SpiApi
public interface ILaserRenderer {
    default void beginRender() {}

    default void endRender() {}

    default boolean canBatch() { return true; }

    default void renderBatch(Collection<ClientLaser> lasers, float partialTicks, double viewX, double viewY, double viewZ) {
        for (ClientLaser laser : lasers) {
            render(laser, partialTicks, viewX, viewY, viewZ);
        }
    }

    void render(ClientLaser laser, float partialTicks, double viewX, double viewY, double viewZ);

    default void deleteGlResources() {}
}
