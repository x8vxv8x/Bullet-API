package com.smd.bulletapi.common.runtime.summon;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.network.PacketHandler;
import com.smd.bulletapi.network.SPacketBulletVisual;
import com.smd.bulletapi.network.SPacketSummon;
import com.smd.bulletapi.server.summon.SummonBullet;
import net.minecraft.world.World;

@InternalApi
public final class SummonSyncService {
    public void sendSpawn(World world, SummonBullet summon) {
        PacketHandler.sendToDimension(SPacketSummon.createSpawn(summon), world.provider.getDimension());
    }

    public void sendRemove(World world, int id) {
        PacketHandler.sendToDimension(SPacketSummon.createRemove(id), world.provider.getDimension());
    }

    public void sendSnapshot(World world, SummonBullet summon, int flags) {
        if (flags == 0) return;
        PacketHandler.sendToDimension(
                SPacketSummon.createSnapshot(
                        summon.getId(),
                        flags,
                        (flags & SPacketSummon.FLAG_POSITION) != 0 ? summon.getPosition() : null,
                        (flags & SPacketSummon.FLAG_VELOCITY) != 0 ? summon.getVelocity() : null,
                        (flags & SPacketSummon.FLAG_LIFE) != 0 ? summon.getLife() : null
                ),
                world.provider.getDimension()
        );
    }

    public void sendVisual(World world, SummonBullet summon, int flags) {
        if (flags == 0) return;
        PacketHandler.sendToDimension(
                SPacketBulletVisual.createSummon(
                        summon.getId(),
                        flags,
                        (flags & SPacketBulletVisual.FLAG_TEXTURE) != 0 ? summon.getTexture() : null,
                        (flags & SPacketBulletVisual.FLAG_RENDERER) != 0 ? summon.getRendererType() : null,
                        (flags & SPacketBulletVisual.FLAG_RENDER_STATE) != 0 ? summon.getRenderState() : null
                ),
                world.provider.getDimension()
        );
    }
}
