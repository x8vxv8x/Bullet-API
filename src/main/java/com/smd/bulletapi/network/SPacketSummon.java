package com.smd.bulletapi.network;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.client.ClientSummonCache;
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
public class SPacketSummon implements IMessage {
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
    private NBTTagCompound customData;

    public SPacketSummon() {}

    private SPacketSummon(Operation op, int id) {
        this.op = op;
        this.id = id;
    }

    public static SPacketSummon createSpawn(int id, Vec3d pos, Vec3d vel, int life, float damage,
                                            String texture, int color, float size, String rendererType,
                                            NBTTagCompound customData) {
        SPacketSummon packet = new SPacketSummon(Operation.SPAWN, id);
        packet.x = pos.x;
        packet.y = pos.y;
        packet.z = pos.z;
        packet.vx = vel.x;
        packet.vy = vel.y;
        packet.vz = vel.z;
        packet.life = life;
        packet.damage = damage;
        packet.texture = texture;
        packet.color = color;
        packet.size = size;
        packet.rendererType = rendererType;
        packet.customData = customData;
        return packet;
    }

    public static SPacketSummon createSnapshot(int id, Vec3d pos, Vec3d vel, int life) {
        SPacketSummon packet = new SPacketSummon(Operation.SNAPSHOT, id);
        packet.x = pos.x;
        packet.y = pos.y;
        packet.z = pos.z;
        packet.vx = vel.x;
        packet.vy = vel.y;
        packet.vz = vel.z;
        packet.life = life;
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
                texture = ByteBufUtils.readUTF8String(buf);
                color = buf.readInt();
                size = buf.readFloat();
                rendererType = ByteBufUtils.readUTF8String(buf);
                customData = ByteBufUtils.readTag(buf);
                break;
            case SNAPSHOT:
                x = buf.readDouble();
                y = buf.readDouble();
                z = buf.readDouble();
                vx = buf.readDouble();
                vy = buf.readDouble();
                vz = buf.readDouble();
                life = buf.readInt();
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
                ByteBufUtils.writeUTF8String(buf, texture == null ? "" : texture);
                buf.writeInt(color);
                buf.writeFloat(size);
                ByteBufUtils.writeUTF8String(buf, rendererType == null ? "" : rendererType);
                ByteBufUtils.writeTag(buf, customData);
                break;
            case SNAPSHOT:
                buf.writeDouble(x);
                buf.writeDouble(y);
                buf.writeDouble(z);
                buf.writeDouble(vx);
                buf.writeDouble(vy);
                buf.writeDouble(vz);
                buf.writeInt(life);
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
                if (cache == null) return;
                switch (message.op) {
                    case SPAWN:
                        ResourceLocation textureLocation = null;
                        if (message.texture != null && !message.texture.isEmpty()) {
                            textureLocation = new ResourceLocation(message.texture);
                        }
                        cache.spawnSummon(
                                message.id,
                                new Vec3d(message.x, message.y, message.z),
                                new Vec3d(message.vx, message.vy, message.vz),
                                message.life,
                                message.damage,
                                textureLocation,
                                message.color,
                                message.size,
                                message.rendererType,
                                message.customData
                        );
                        break;
                    case SNAPSHOT:
                        cache.updateSummon(
                                message.id,
                                new Vec3d(message.x, message.y, message.z),
                                new Vec3d(message.vx, message.vy, message.vz),
                                message.life
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
