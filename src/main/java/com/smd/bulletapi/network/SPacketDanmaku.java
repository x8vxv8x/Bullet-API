package com.smd.bulletapi.network;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.client.ClientDanmakuCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@InternalApi
public class SPacketDanmaku implements IMessage {
    public static final int FLAG_POSITION = 1;
    public static final int FLAG_VELOCITY = 1 << 1;
    public static final int FLAG_LIFE = 1 << 2;

    public enum Operation {
        SPAWN, UPDATE, REMOVE
    }

    private Operation op;
    private int id;
    // SPAWN 数据
    private double x, y, z;
    private double vx, vy, vz;
    private int life;
    private float damage;
    private String texture;
    private int color;               // 新增
    private float size;               // 新增
    private String rendererType;       // 新增
    private NBTTagCompound customData;
    private int flags;
    public SPacketDanmaku() {}

    private SPacketDanmaku(Operation op, int id) {
        this.op = op;
        this.id = id;
    }

    public static SPacketDanmaku createSpawn(int id, Vec3d pos, Vec3d vel, int life, float damage,
                                             String texture, int color, float size, String rendererType,
                                             NBTTagCompound customData) {
        SPacketDanmaku p = new SPacketDanmaku(Operation.SPAWN, id);
        p.x = pos.x; p.y = pos.y; p.z = pos.z;
        p.vx = vel.x; p.vy = vel.y; p.vz = vel.z;
        p.life = life;
        p.damage = damage;
        p.texture = texture;
        p.color = color;
        p.size = size;
        p.rendererType = rendererType;
        p.customData = customData;
        return p;
    }

    public static SPacketDanmaku createUpdate(int id, int flags, Vec3d position, Vec3d velocity, Integer life) {
        SPacketDanmaku p = new SPacketDanmaku(Operation.UPDATE, id);
        p.flags = flags;
        if ((flags & FLAG_POSITION) != 0 && position != null) {
            p.x = position.x; p.y = position.y; p.z = position.z;
        }
        if ((flags & FLAG_VELOCITY) != 0 && velocity != null) {
            p.vx = velocity.x; p.vy = velocity.y; p.vz = velocity.z;
        }
        if ((flags & FLAG_LIFE) != 0 && life != null) {
            p.life = life;
        }
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
                x = buf.readDouble(); y = buf.readDouble(); z = buf.readDouble();
                vx = buf.readDouble(); vy = buf.readDouble(); vz = buf.readDouble();
                life = buf.readInt();
                damage = buf.readFloat();
                texture = ByteBufUtils.readUTF8String(buf);
                color = buf.readInt();
                size = buf.readFloat();
                rendererType = ByteBufUtils.readUTF8String(buf);
                customData = ByteBufUtils.readTag(buf);
                break;
            case UPDATE:
                flags = buf.readInt();
                if ((flags & FLAG_POSITION) != 0) {
                    x = buf.readDouble(); y = buf.readDouble(); z = buf.readDouble();
                }
                if ((flags & FLAG_VELOCITY) != 0) {
                    vx = buf.readDouble(); vy = buf.readDouble(); vz = buf.readDouble();
                }
                if ((flags & FLAG_LIFE) != 0) {
                    life = buf.readInt();
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
                buf.writeDouble(x); buf.writeDouble(y); buf.writeDouble(z);
                buf.writeDouble(vx); buf.writeDouble(vy); buf.writeDouble(vz);
                buf.writeInt(life);
                buf.writeFloat(damage);
                ByteBufUtils.writeUTF8String(buf, texture == null ? "" : texture);
                buf.writeInt(color);
                buf.writeFloat(size);
                ByteBufUtils.writeUTF8String(buf, rendererType == null ? "" : rendererType);
                ByteBufUtils.writeTag(buf, customData);
                break;
            case UPDATE:
                buf.writeInt(flags);
                if ((flags & FLAG_POSITION) != 0) {
                    buf.writeDouble(x); buf.writeDouble(y); buf.writeDouble(z);
                }
                if ((flags & FLAG_VELOCITY) != 0) {
                    buf.writeDouble(vx); buf.writeDouble(vy); buf.writeDouble(vz);
                }
                if ((flags & FLAG_LIFE) != 0) {
                    buf.writeInt(life);
                }
                break;
            case REMOVE:
                break;
        }
    }

    public static class Handler implements IMessageHandler<SPacketDanmaku, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(SPacketDanmaku message, MessageContext ctx) {
            PacketHandler.runOnClientThread(ctx, () -> {
                ClientDanmakuCache cache = ClientDanmakuCache.INSTANCE;
                if (cache == null) return;
                switch (message.op) {
                    case SPAWN:
                        ResourceLocation tex = null;
                        if (message.texture != null && !message.texture.isEmpty()) {
                            tex = new ResourceLocation(message.texture);
                        }
                        cache.spawnBullet(
                                message.id,
                                new Vec3d(message.x, message.y, message.z),
                                new Vec3d(message.vx, message.vy, message.vz),
                                message.life,
                                message.damage,
                                tex,
                                message.color,
                                message.size,
                                message.rendererType,
                                message.customData
                        );
                        break;
                    case UPDATE:
                        cache.updateBullet(
                                message.id,
                                (message.flags & FLAG_POSITION) != 0 ? new Vec3d(message.x, message.y, message.z) : null,
                                (message.flags & FLAG_VELOCITY) != 0 ? new Vec3d(message.vx, message.vy, message.vz) : null,
                                (message.flags & FLAG_LIFE) != 0 ? Integer.valueOf(message.life) : null
                        );
                        break;
                    case REMOVE:
                        cache.removeBullet(message.id);
                        break;
                }
            });
            return null;
        }
    }
}
