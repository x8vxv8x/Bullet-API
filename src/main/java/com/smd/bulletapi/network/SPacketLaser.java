package com.smd.bulletapi.network;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.client.ClientLaserCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@InternalApi
public class SPacketLaser implements IMessage {
    public static final int FLAG_START = 1;
    public static final int FLAG_DIRECTION = 1 << 1;
    public static final int FLAG_LENGTH = 1 << 2;

    public enum Operation {
        SPAWN, UPDATE, REMOVE
    }

    private Operation op;
    private int id;
    private long tick;
    private double sx, sy, sz;
    private double dx, dy, dz;
    private double length;
    private float thickness;
    private int color;
    private String rendererType;
    private NBTTagCompound customData;
    private int flags;

    public SPacketLaser() {}

    private SPacketLaser(Operation op, int id) {
        this.op = op;
        this.id = id;
    }

    public static SPacketLaser createSpawn(int id, long tick, Vec3d start, Vec3d dir, double length,
                                           float thickness, int color, String rendererType,
                                           NBTTagCompound customData) {
        SPacketLaser p = new SPacketLaser(Operation.SPAWN, id);
        p.tick = tick;
        p.sx = start.x; p.sy = start.y; p.sz = start.z;
        p.dx = dir.x; p.dy = dir.y; p.dz = dir.z;
        p.length = length;
        p.thickness = thickness;
        p.color = color;
        p.rendererType = rendererType;
        p.customData = customData;
        return p;
    }

    public static SPacketLaser createUpdate(int id, long tick, int flags, Vec3d start, Vec3d dir, Double length) {
        SPacketLaser p = new SPacketLaser(Operation.UPDATE, id);
        p.tick = tick;
        p.flags = flags;
        if ((flags & FLAG_START) != 0 && start != null) {
            p.sx = start.x; p.sy = start.y; p.sz = start.z;
        }
        if ((flags & FLAG_DIRECTION) != 0 && dir != null) {
            p.dx = dir.x; p.dy = dir.y; p.dz = dir.z;
        }
        if ((flags & FLAG_LENGTH) != 0 && length != null) {
            p.length = length;
        }
        return p;
    }

    public static SPacketLaser createRemove(int id) {
        return new SPacketLaser(Operation.REMOVE, id);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        op = Operation.values()[buf.readInt()];
        id = buf.readInt();
        switch (op) {
            case SPAWN:
                tick = buf.readLong();
                sx = buf.readDouble(); sy = buf.readDouble(); sz = buf.readDouble();
                dx = buf.readDouble(); dy = buf.readDouble(); dz = buf.readDouble();
                length = buf.readDouble();
                thickness = buf.readFloat();
                color = buf.readInt();
                rendererType = ByteBufUtils.readUTF8String(buf);
                customData = ByteBufUtils.readTag(buf);
                break;
            case UPDATE:
                tick = buf.readLong();
                flags = buf.readInt();
                if ((flags & FLAG_START) != 0) {
                    sx = buf.readDouble(); sy = buf.readDouble(); sz = buf.readDouble();
                }
                if ((flags & FLAG_DIRECTION) != 0) {
                    dx = buf.readDouble(); dy = buf.readDouble(); dz = buf.readDouble();
                }
                if ((flags & FLAG_LENGTH) != 0) {
                    length = buf.readDouble();
                }
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
                buf.writeLong(tick);
                buf.writeDouble(sx); buf.writeDouble(sy); buf.writeDouble(sz);
                buf.writeDouble(dx); buf.writeDouble(dy); buf.writeDouble(dz);
                buf.writeDouble(length);
                buf.writeFloat(thickness);
                buf.writeInt(color);
                ByteBufUtils.writeUTF8String(buf, rendererType == null ? "" : rendererType);
                ByteBufUtils.writeTag(buf, customData);
                break;
            case UPDATE:
                buf.writeLong(tick);
                buf.writeInt(flags);
                if ((flags & FLAG_START) != 0) {
                    buf.writeDouble(sx); buf.writeDouble(sy); buf.writeDouble(sz);
                }
                if ((flags & FLAG_DIRECTION) != 0) {
                    buf.writeDouble(dx); buf.writeDouble(dy); buf.writeDouble(dz);
                }
                if ((flags & FLAG_LENGTH) != 0) {
                    buf.writeDouble(length);
                }
                break;
            case REMOVE:
                break;
        }
    }

    public static class Handler implements IMessageHandler<SPacketLaser, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(SPacketLaser message, MessageContext ctx) {
            PacketHandler.runOnClientThread(ctx, () -> {
                ClientLaserCache cache = ClientLaserCache.INSTANCE;
                if (cache == null) return;
                switch (message.op) {
                    case SPAWN:
                        cache.spawnLaser(
                                message.id,
                                message.tick,
                                new Vec3d(message.sx, message.sy, message.sz),
                                new Vec3d(message.dx, message.dy, message.dz),
                                message.length,
                                message.thickness,
                                message.color,
                                message.rendererType,
                                message.customData
                        );
                        break;
                    case UPDATE:
                        cache.updateLaser(
                                message.id,
                                message.tick,
                                message.flags,
                                (message.flags & FLAG_START) != 0 ? new Vec3d(message.sx, message.sy, message.sz) : null,
                                (message.flags & FLAG_DIRECTION) != 0 ? new Vec3d(message.dx, message.dy, message.dz) : null,
                                (message.flags & FLAG_LENGTH) != 0 ? Double.valueOf(message.length) : null
                        );
                        break;
                    case REMOVE:
                        cache.removeLaser(message.id);
                        break;
                }
            });
            return null;
        }
    }
}
