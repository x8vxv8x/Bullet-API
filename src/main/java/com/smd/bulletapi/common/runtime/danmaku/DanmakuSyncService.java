package com.smd.bulletapi.common.runtime.danmaku;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.network.PacketHandler;
import com.smd.bulletapi.network.SPacketBulletVisual;
import com.smd.bulletapi.network.SPacketDanmaku;
import com.smd.bulletapi.network.SPacketLaser;
import com.smd.bulletapi.server.Bullet;
import com.smd.bulletapi.server.Laser;
import net.minecraft.world.World;

@InternalApi
public final class DanmakuSyncService {
    public void sendBulletSpawn(World world, Bullet bullet) {
        PacketHandler.sendToDimension(SPacketDanmaku.createSpawn(bullet), world.provider.getDimension());
    }

    public void sendBulletRemove(World world, int id) {
        PacketHandler.sendToDimension(SPacketDanmaku.createRemove(id), world.provider.getDimension());
    }

    public void syncBullet(World world, Bullet bullet, int flags) {
        if (flags == 0) return;
        PacketHandler.sendToDimension(
                SPacketDanmaku.createUpdate(
                        bullet.getId(),
                        flags,
                        (flags & SPacketDanmaku.FLAG_POSITION) != 0 ? bullet.getPosition() : null,
                        (flags & SPacketDanmaku.FLAG_VELOCITY) != 0 ? bullet.getVelocity() : null,
                        (flags & SPacketDanmaku.FLAG_LIFE) != 0 ? bullet.getLife() : null
                ),
                world.provider.getDimension()
        );
    }

    public void syncBulletVisual(World world, Bullet bullet, int flags) {
        if (flags == 0) return;
        PacketHandler.sendToDimension(
                SPacketBulletVisual.createBullet(
                        bullet.getId(),
                        flags,
                        (flags & SPacketBulletVisual.FLAG_TEXTURE) != 0 ? bullet.getTexture() : null,
                        (flags & SPacketBulletVisual.FLAG_RENDERER) != 0 ? bullet.getRendererType() : null,
                        (flags & SPacketBulletVisual.FLAG_RENDER_STATE) != 0 ? bullet.getRenderState() : null
                ),
                world.provider.getDimension()
        );
    }

    public void sendLaserSpawn(World world, Laser laser) {
        PacketHandler.sendToDimension(
                SPacketLaser.createSpawn(laser, world.getTotalWorldTime()),
                world.provider.getDimension()
        );
    }

    public void sendLaserRemove(World world, int id) {
        PacketHandler.sendToDimension(SPacketLaser.createRemove(id), world.provider.getDimension());
    }

    public void syncLaser(World world, Laser laser, int flags) {
        if (flags == 0) return;
        PacketHandler.sendToDimension(
                SPacketLaser.createUpdate(
                        laser.getId(),
                        world.getTotalWorldTime(),
                        flags,
                        (flags & SPacketLaser.FLAG_START) != 0 ? laser.getStart() : null,
                        (flags & SPacketLaser.FLAG_DIRECTION) != 0 ? laser.getDirection() : null,
                        (flags & SPacketLaser.FLAG_LENGTH) != 0 ? laser.getCurrentLength() : null
                ),
                world.provider.getDimension()
        );
    }
}
