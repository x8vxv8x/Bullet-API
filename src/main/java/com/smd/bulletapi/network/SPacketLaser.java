package com.smd.bulletapi.network;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.client.ClientLaserCache;
import com.smd.bulletapi.common.RenderDataRefs;
import com.smd.bulletapi.server.Laser;
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
    private boolean usePresetRef;
    private String presetId;
    private int flags;

    public SPacketLaser() {}

    private SPacketLaser(Operation op, int id) {
        this.op = op;
        this.id = id;
    }

    public static SPacketLaser createSpawn(Laser laser, long tick) {
        SPacketLaser p = new SPacketLaser(Operation.SPAWN, laser.getId());
        p.tick = tick;
        Vec3d start = laser.getStart();
        Vec3d dir = laser.getDirection();
        p.sx = start.x; p.sy = start.y; p.sz = start.z;
        p.dx = dir.x; p.dy = dir.y; p.dz = dir.z;
        p.length = laser.getCurrentLength();

        String renderPresetId = laser.getRenderPresetId();
        RenderDataRefs.LaserRenderData actual = RenderDataRefs.laserFromRuntime(laser);
        RenderDataRefs.LaserRenderData base = RenderDataRefs.laserFromPreset(renderPresetId);
        if (renderPresetId != null && base != null) {
            NBTTagCompound customDataDiff = RenderDataRefs.diff(actual.customData, base.customData);
            p.usePresetRef = true;
            p.presetId = renderPresetId;
            p.flags = RenderDataRefs.laserDiffFlags(actual, base, customDataDiff);
            if ((p.flags & RenderDataRefs.FLAG_THICKNESS) != 0) p.thickness = actual.thickness;
            if ((p.flags & RenderDataRefs.FLAG_COLOR) != 0) p.color = actual.color;
            if ((p.flags & RenderDataRefs.FLAG_RENDERER) != 0) p.rendererType = actual.rendererType;
            if ((p.flags & RenderDataRefs.FLAG_CUSTOM_DATA) != 0) {
                p.customData = customDataDiff;
            }
        } else {
            p.thickness = actual.thickness;
            p.color = actual.color;
            p.rendererType = actual.rendererType;
            p.customData = actual.customData;
        }
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
                usePresetRef = buf.readBoolean();
                if (usePresetRef) {
                    presetId = ByteBufUtils.readUTF8String(buf);
                    flags = buf.readInt();
                    if ((flags & RenderDataRefs.FLAG_THICKNESS) != 0) thickness = buf.readFloat();
                    if ((flags & RenderDataRefs.FLAG_COLOR) != 0) color = buf.readInt();
                    if ((flags & RenderDataRefs.FLAG_RENDERER) != 0) rendererType = ByteBufUtils.readUTF8String(buf);
                    if ((flags & RenderDataRefs.FLAG_CUSTOM_DATA) != 0) customData = ByteBufUtils.readTag(buf);
                } else {
                    thickness = buf.readFloat();
                    color = buf.readInt();
                    rendererType = ByteBufUtils.readUTF8String(buf);
                    customData = ByteBufUtils.readTag(buf);
                }
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
                buf.writeBoolean(usePresetRef);
                if (usePresetRef) {
                    ByteBufUtils.writeUTF8String(buf, presetId == null ? "" : presetId);
                    buf.writeInt(flags);
                    if ((flags & RenderDataRefs.FLAG_THICKNESS) != 0) buf.writeFloat(thickness);
                    if ((flags & RenderDataRefs.FLAG_COLOR) != 0) buf.writeInt(color);
                    if ((flags & RenderDataRefs.FLAG_RENDERER) != 0) {
                        ByteBufUtils.writeUTF8String(buf, rendererType == null ? "" : rendererType);
                    }
                    if ((flags & RenderDataRefs.FLAG_CUSTOM_DATA) != 0) {
                        ByteBufUtils.writeTag(buf, customData);
                    }
                } else {
                    buf.writeFloat(thickness);
                    buf.writeInt(color);
                    ByteBufUtils.writeUTF8String(buf, rendererType == null ? "" : rendererType);
                    ByteBufUtils.writeTag(buf, customData);
                }
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
                        float thicknessValue = message.thickness;
                        int colorValue = message.color;
                        String rendererTypeValue = message.rendererType;
                        NBTTagCompound customDataValue = message.customData;
                        if (message.usePresetRef) {
                            RenderDataRefs.LaserRenderData base = RenderDataRefs.laserFromPreset(message.presetId);
                            if (base != null) {
                                if ((message.flags & RenderDataRefs.FLAG_THICKNESS) == 0) thicknessValue = base.thickness;
                                if ((message.flags & RenderDataRefs.FLAG_COLOR) == 0) colorValue = base.color;
                                if ((message.flags & RenderDataRefs.FLAG_RENDERER) == 0) rendererTypeValue = base.rendererType;
                                customDataValue = (message.flags & RenderDataRefs.FLAG_CUSTOM_DATA) != 0
                                        ? RenderDataRefs.merge(base.customData, message.customData)
                                        : base.customData;
                            }
                        }
                        cache.spawnLaser(
                                message.id,
                                message.tick,
                                new Vec3d(message.sx, message.sy, message.sz),
                                new Vec3d(message.dx, message.dy, message.dz),
                                message.length,
                                thicknessValue,
                                colorValue,
                                rendererTypeValue,
                                customDataValue
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
