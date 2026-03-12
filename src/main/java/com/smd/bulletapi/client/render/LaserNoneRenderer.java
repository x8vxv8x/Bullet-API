package com.smd.bulletapi.client.render;

import com.smd.bulletapi.client.ClientLaser;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collection;

@SideOnly(Side.CLIENT)
public class LaserNoneRenderer implements ILaserRenderer {
    public static final LaserNoneRenderer INSTANCE = new LaserNoneRenderer();
    private LaserNoneRenderer() {}

    @Override
    public void renderBatch(Collection<ClientLaser> lasers, float partialTicks, double viewX, double viewY, double viewZ) {
        // Intentionally empty: developer-owned rendering
    }

    @Override
    public void render(ClientLaser laser, float partialTicks, double viewX, double viewY, double viewZ) {
        // Intentionally empty: developer-owned rendering
    }
}
