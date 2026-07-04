package com.smd.bulletapi.network;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.client.ClientSummonCache;
import com.smd.bulletapi.common.RenderDataRefs;
import com.smd.bulletapi.common.data.DataPayload;
import com.smd.bulletapi.server.summon.SummonBullet;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@InternalApi
public class SPacketSummon implements IMessage {
    public static final int FLAG_POSITION = 1;
    public static final int FLAG_VELOCITY = 1 << 1;
    public static final int FLAG_LIFE = 1 << 2;

    public enum Operation {
        SPAWN, SNAPSHOT, REMOVE
    }

    private Operation op;
    private int id;
    private double x, y, z;
    private double vx, vy, vz;
    private int life;
    private float damage;
    private String texture;
    private int color;
    private float size;
    private String rendererType;
    private DataPayload customData;
    private boolean useDefinitionRef;
    private String definitionId;
    private int flags;

    public SPacketSummon() {}

    private SPacketSummon(Operation op, int id) {
        this.op = op;
        this.id = id;
    }

    public static SPacketSummon createSpawn(SummonBullet summon) {
        SPacketSummon packet = new SPacketSummon(Operation.SPAWN, summon.getId());
        packet.x = summon.getPosX();
        packet.y = summon.getPosY();
        packet.z = summon.getPosZ();
        packet.vx = summon.getVelX();
        packet.vy = summon.getVelY();
        packet.vz = summon.getVelZ();
        packet.life = summon.getLife();
        packet.damage = summon.getDamage();

        String refDefinitionId = summon.getDefinitionId();
        RenderDataRefs.BulletRenderData actual = RenderDataRefs.summonFromRuntime(summon);
        RenderDataRefs.BulletRenderData base = RenderDataRefs.summonFromDefinition(refDefinitionId);
        if (refDefinitionId != null && base != null) {
            DataPayload customDataDiff = RenderDataRefs.diff(actual.customData, base.customData);
            packet.useDefinitionRef = true;
            packet.definitionId = refDefinitionId;
            packet.flags = RenderDataRefs.bulletDiffFlags(actual, base, customDataDiff);
            if ((packet.flags & RenderDataRefs.FLAG_TEXTURE) != 0) {
                packet.texture = actual.texture;
            }
            if ((packet.flags & RenderDataRefs.FLAG_COLOR) != 0) {
                packet.color = actual.color;
            }
            if ((packet.flags & RenderDataRefs.FLAG_SIZE) != 0) {
                packet.size = actual.size;
            }
            if ((packet.flags & RenderDataRefs.FLAG_RENDERER) != 0) {
                packet.rendererType = actual.rendererType;
            }
            if ((packet.flags & RenderDataRefs.FLAG_CUSTOM_DATA) != 0) {
                packet.customData = customDataDiff;
            }
        } else {
            packet.texture = actual.texture;
            packet.color = actual.color;
            packet.size = actual.size;
            packet.rendererType = actual.rendererType;
            packet.customData = actual.customData;
        }
        return packet;
    }

    public static SPacketSummon createSnapshot(int id, int flags,
                                               double x, double y, double z,
                                               double vx, double vy, double vz,
                                               Integer life) {
        SPacketSummon packet = new SPacketSummon(Operation.SNAPSHOT, id);
        packet.flags = flags;
        if ((flags & FLAG_POSITION) != 0) {
            packet.x = x;
            packet.y = y;
            packet.z = z;
        }
        if ((flags & FLAG_VELOCITY) != 0) {
            packet.vx = vx;
            packet.vy = vy;
            packet.vz = vz;
        }
        if ((flags & FLAG_LIFE) != 0 && life != null) {
            packet.life = life;
        }
        return packet;
    }

    public static SPacketSummon createRemove(int id) {
        return new SPacketSummon(Operation.REMOVE, id);
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
                useDefinitionRef = buf.readBoolean();
                if (useDefinitionRef) {
                    definitionId = ByteBufUtils.readUTF8String(buf);
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
            case SNAPSHOT:
                flags = buf.readInt();
                if ((flags & FLAG_POSITION) != 0) {
                    x = buf.readDouble();
                    y = buf.readDouble();
                    z = buf.readDouble();
                }
                if ((flags & FLAG_VELOCITY) != 0) {
                    vx = buf.readDouble();
                    vy = buf.readDouble();
                    vz = buf.readDouble();
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
                buf.writeDouble(x);
                buf.writeDouble(y);
                buf.writeDouble(z);
                buf.writeDouble(vx);
                buf.writeDouble(vy);
                buf.writeDouble(vz);
                buf.writeInt(life);
                buf.writeFloat(damage);
                buf.writeBoolean(useDefinitionRef);
                if (useDefinitionRef) {
                    ByteBufUtils.writeUTF8String(buf, definitionId == null ? "" : definitionId);
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
            case SNAPSHOT:
                buf.writeInt(flags);
                if ((flags & FLAG_POSITION) != 0) {
                    buf.writeDouble(x);
                    buf.writeDouble(y);
                    buf.writeDouble(z);
                }
                if ((flags & FLAG_VELOCITY) != 0) {
                    buf.writeDouble(vx);
                    buf.writeDouble(vy);
                    buf.writeDouble(vz);
                }
                if ((flags & FLAG_LIFE) != 0) {
                    buf.writeInt(life);
                }
                break;
            case REMOVE:
                break;
        }
    }

    public static class Handler implements IMessageHandler<SPacketSummon, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(SPacketSummon message, MessageContext ctx) {
            PacketHandler.runOnClientThread(ctx, () -> {
                ClientSummonCache cache = ClientSummonCache.INSTANCE;
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
                        if (message.useDefinitionRef) {
                            RenderDataRefs.BulletRenderData base = RenderDataRefs.summonFromDefinition(message.definitionId);
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
                        ResourceLocation textureLocation = null;
                        if (textureValue != null && !textureValue.isEmpty()) {
                            textureLocation = new ResourceLocation(textureValue);
                        }
                        cache.spawnSummon(
                                message.id,
                                message.x,
                                message.y,
                                message.z,
                                message.vx,
                                message.vy,
                                message.vz,
                                message.life,
                                message.damage,
                                textureLocation,
                                colorValue,
                                sizeValue,
                                rendererTypeValue,
                                customDataValue
                        );
                        break;
                    case SNAPSHOT:
                        cache.updateSummon(
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
                        cache.removeSummon(message.id);
                        break;
                }
            });
            return null;
        }
    }
}
