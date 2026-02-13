package com.smd.bulletapi.network;


import com.smd.bulletapi.client.ClientDanmakuCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class SPacketDanmaku implements IMessage {
    public enum Operation {
        SPAWN, UPDATE, REMOVE
    }

    private Operation op;
    private int id;
    private double x, y, z;     // 仅 SPAWN 用（位置）
    private double vx, vy, vz;   // SPAWN(速度) / UPDATE(速度)
    private int life;            // SPAWN(最大生命)
    private float damage;        // SPAWN(伤害)

    public SPacketDanmaku() {}

    private SPacketDanmaku(Operation op, int id) {
        this.op = op;
        this.id = id;
    }

    public static SPacketDanmaku createSpawn(int id, Vec3d pos, Vec3d vel, int life, float damage) {
        SPacketDanmaku p = new SPacketDanmaku(Operation.SPAWN, id);
        p.x = pos.x; p.y = pos.y; p.z = pos.z;
        p.vx = vel.x; p.vy = vel.y; p.vz = vel.z;
        p.life = life;
        p.damage = damage;
        return p;
    }

    public static SPacketDanmaku createUpdate(int id, Vec3d velocity) {
        SPacketDanmaku p = new SPacketDanmaku(Operation.UPDATE, id);
        p.vx = velocity.x; p.vy = velocity.y; p.vz = velocity.z;
        return p;
    }

    public static SPacketDanmaku createRemove(int id) {
        return new SPacketDanmaku(Operation.REMOVE, id);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        op = Operation.values()[buf.readInt()];
        id = buf.readInt();
        switch (op) {
            case SPAWN:
                x = buf.readDouble();
                y = buf.readDouble();
                z = buf.readDouble();
                vx = buf.readDouble();
                vy = buf.readDouble();
                vz = buf.readDouble();
                life = buf.readInt();
                damage = buf.readFloat();
                break;
            case UPDATE:
                vx = buf.readDouble();
                vy = buf.readDouble();
                vz = buf.readDouble();
                break;
            case REMOVE:
                break;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(op.ordinal());
        buf.writeInt(id);
        switch (op) {
            case SPAWN:
                buf.writeDouble(x);
                buf.writeDouble(y);
                buf.writeDouble(z);
                buf.writeDouble(vx);
                buf.writeDouble(vy);
                buf.writeDouble(vz);
                buf.writeInt(life);
                buf.writeFloat(damage);
                break;
            case UPDATE:
                buf.writeDouble(vx);
                buf.writeDouble(vy);
                buf.writeDouble(vz);
                break;
            case REMOVE:
                break;
        }
    }

    public static class Handler implements IMessageHandler<SPacketDanmaku, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(SPacketDanmaku message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                net.minecraftforge.fml.common.FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    System.out.println("[BulletAPI] 客户端收到包，操作：" + message.op);
                    ClientDanmakuCache cache = ClientDanmakuCache.INSTANCE;
                    if (cache == null) return;
                    switch (message.op) {
                        case SPAWN:
                            cache.spawnBullet(message.id,
                                    new Vec3d(message.x, message.y, message.z),
                                    new Vec3d(message.vx, message.vy, message.vz),
                                    message.life, message.damage);
                            break;
                        case UPDATE:
                            cache.updateBulletVelocity(message.id,
                                    new Vec3d(message.vx, message.vy, message.vz));
                            break;
                        case REMOVE:
                            cache.removeBullet(message.id);
                            break;
                    }
                });
            }
            return null;
        }
    }
}