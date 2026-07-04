package com.smd.bulletapi.network;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.client.ClientDanmakuCache;
import com.smd.bulletapi.common.RenderDataRefs;
import com.smd.bulletapi.common.data.DataPayload;
import com.smd.bulletapi.server.Bullet;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
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
    private DataPayload customData;
    private boolean usePresetRef;
    private String presetId;
    private int flags;
    public SPacketDanmaku() {}

    private SPacketDanmaku(Operation op, int id) {
        this.op = op;
        this.id = id;
    }

    public static SPacketDanmaku createSpawn(Bullet bullet) {
        SPacketDanmaku p = new SPacketDanmaku(Operation.SPAWN, bullet.getId());
        p.x = bullet.getPosX(); p.y = bullet.getPosY(); p.z = bullet.getPosZ();
        p.vx = bullet.getVelX(); p.vy = bullet.getVelY(); p.vz = bullet.getVelZ();
        p.life = bullet.getLife();
        p.damage = bullet.getDamage();

        String renderPresetId = bullet.getRenderPresetId();
        RenderDataRefs.BulletRenderData actual = RenderDataRefs.bulletFromRuntime(bullet);
        RenderDataRefs.BulletRenderData base = RenderDataRefs.bulletFromPreset(renderPresetId);
        if (renderPresetId != null && base != null) {
            DataPayload customDataDiff = RenderDataRefs.diff(actual.customData, base.customData);
            p.usePresetRef = true;
            p.presetId = renderPresetId;
            p.flags = RenderDataRefs.bulletDiffFlags(actual, base, customDataDiff);
            if ((p.flags & RenderDataRefs.FLAG_TEXTURE) != 0) {
                p.texture = actual.texture;
            }
            if ((p.flags & RenderDataRefs.FLAG_COLOR) != 0) {
                p.color = actual.color;
            }
            if ((p.flags & RenderDataRefs.FLAG_SIZE) != 0) {
                p.size = actual.size;
            }
            if ((p.flags & RenderDataRefs.FLAG_RENDERER) != 0) {
                p.rendererType = actual.rendererType;
            }
            if ((p.flags & RenderDataRefs.FLAG_CUSTOM_DATA) != 0) {
                p.customData = customDataDiff;
            }
        } else {
            p.texture = actual.texture;
            p.color = actual.color;
            p.size = actual.size;
            p.rendererType = actual.rendererType;
            p.customData = actual.customData;
        }
        return p;
    }

    public static SPacketDanmaku createUpdate(int id, int flags,
                                              double x, double y, double z,
                                              double vx, double vy, double vz,
                                              Integer life) {
        SPacketDanmaku p = new SPacketDanmaku(Operation.UPDATE, id);
        p.flags = flags;
        if ((flags & FLAG_POSITION) != 0) {
            p.x = x; p.y = y; p.z = z;
        }
        if ((flags & FLAG_VELOCITY) != 0) {
            p.vx = vx; p.vy = vy; p.vz = vz;
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
                usePresetRef = buf.readBoolean();
                if (usePresetRef) {
                    presetId = ByteBufUtils.readUTF8String(buf);
                    flags = buf.readInt();
                    if ((flags & RenderDataRefs.FLAG_TEXTURE) != 0) {
                        texture = ByteBufUtils.readUTF8String(buf);
                    }
                    if ((flags & RenderDataRefs.FLAG_COLOR) != 0) {
                        color = buf.readInt();
                    }
                    if ((flags & RenderDataRefs.FLAG_SIZE) != 0) {
                        size = buf.readFloat();
                    }
                    if ((flags & RenderDataRefs.FLAG_RENDERER) != 0) {
                        rendererType = ByteBufUtils.readUTF8String(buf);
                    }
                    if ((flags & RenderDataRefs.FLAG_CUSTOM_DATA) != 0) {
                        customData = DataPayload.readFrom(buf);
                    }
                } else {
                    texture = ByteBufUtils.readUTF8String(buf);
                    color = buf.readInt();
                    size = buf.readFloat();
                    rendererType = ByteBufUtils.readUTF8String(buf);
                    customData = DataPayload.readFrom(buf);
                }
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
                buf.writeBoolean(usePresetRef);
                if (usePresetRef) {
                    ByteBufUtils.writeUTF8String(buf, presetId == null ? "" : presetId);
                    buf.writeInt(flags);
                    if ((flags & RenderDataRefs.FLAG_TEXTURE) != 0) {
                        ByteBufUtils.writeUTF8String(buf, texture == null ? "" : texture);
                    }
                    if ((flags & RenderDataRefs.FLAG_COLOR) != 0) {
                        buf.writeInt(color);
                    }
                    if ((flags & RenderDataRefs.FLAG_SIZE) != 0) {
                        buf.writeFloat(size);
                    }
                    if ((flags & RenderDataRefs.FLAG_RENDERER) != 0) {
                        ByteBufUtils.writeUTF8String(buf, rendererType == null ? "" : rendererType);
                    }
                    if ((flags & RenderDataRefs.FLAG_CUSTOM_DATA) != 0) {
                        (customData == null ? new DataPayload() : customData).writeTo(buf);
                    }
                } else {
                    ByteBufUtils.writeUTF8String(buf, texture == null ? "" : texture);
                    buf.writeInt(color);
                    buf.writeFloat(size);
                    ByteBufUtils.writeUTF8String(buf, rendererType == null ? "" : rendererType);
                    (customData == null ? new DataPayload() : customData).writeTo(buf);
                }
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
                if (cache == null) {
                    return;
                }
                switch (message.op) {
                    case SPAWN:
                        String textureValue = message.texture;
                        int colorValue = message.color;
                        float sizeValue = message.size;
                        String rendererTypeValue = message.rendererType;
                        DataPayload customDataValue = message.customData;
                        if (message.usePresetRef) {
                            RenderDataRefs.BulletRenderData base = RenderDataRefs.bulletFromPreset(message.presetId);
                            if (base != null) {
                                if ((message.flags & RenderDataRefs.FLAG_TEXTURE) == 0) {
                                    textureValue = base.texture;
                                }
                                if ((message.flags & RenderDataRefs.FLAG_COLOR) == 0) {
                                    colorValue = base.color;
                                }
                                if ((message.flags & RenderDataRefs.FLAG_SIZE) == 0) {
                                    sizeValue = base.size;
                                }
                                if ((message.flags & RenderDataRefs.FLAG_RENDERER) == 0) {
                                    rendererTypeValue = base.rendererType;
                                }
                                customDataValue = (message.flags & RenderDataRefs.FLAG_CUSTOM_DATA) != 0
                                        ? RenderDataRefs.merge(base.customData, message.customData)
                                        : base.customData;
                            }
                        }
                        ResourceLocation tex = null;
                        if (textureValue != null && !textureValue.isEmpty()) {
                            tex = new ResourceLocation(textureValue);
                        }
                        cache.spawnBullet(
                                message.id,
                                message.x,
                                message.y,
                                message.z,
                                message.vx,
                                message.vy,
                                message.vz,
                                message.life,
                                message.damage,
                                tex,
                                colorValue,
                                sizeValue,
                                rendererTypeValue,
                                customDataValue
                        );
                        break;
                    case UPDATE:
                        cache.updateBullet(
                                message.id,
                                message.flags,
                                message.x,
                                message.y,
                                message.z,
                                message.vx,
                                message.vy,
                                message.vz,
                                (message.flags & FLAG_LIFE) != 0 ? message.life : null
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
