package com.smd.bulletapi.network;


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
    private String texture;          // 新增
    private NBTTagCompound customData; // 新增

    private double nvx, nvy, nvz;

    public SPacketDanmaku() {}

    private SPacketDanmaku(Operation op, int id) {
        this.op = op;
        this.id = id;
    }

    public static SPacketDanmaku createSpawn(int id, Vec3d pos, Vec3d vel, int life, float damage,
                                             String texture, NBTTagCompound customData) {
        SPacketDanmaku p = new SPacketDanmaku();
        p.op = Operation.SPAWN;
        p.id = id;
        p.x = pos.x; p.y = pos.y; p.z = pos.z;
        p.vx = vel.x; p.vy = vel.y; p.vz = vel.z;
        p.life = life;
        p.damage = damage;
        p.texture = texture;
        p.customData = customData;
        return p;
    }

    public static SPacketDanmaku createUpdate(int id, Vec3d velocity) {
        SPacketDanmaku p = new SPacketDanmaku();
        p.op = Operation.UPDATE;
        p.id = id;
        p.nvx = velocity.x; p.nvy = velocity.y; p.nvz = velocity.z;
        return p;
    }

    public static SPacketDanmaku createRemove(int id) {
        SPacketDanmaku p = new SPacketDanmaku();
        p.op = Operation.REMOVE;
        p.id = id;
        return p;
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
                texture = ByteBufUtils.readUTF8String(buf);    // 读取纹理路径
                customData = ByteBufUtils.readTag(buf);        // 读取NBT
                break;
            case UPDATE:
                nvx = buf.readDouble(); nvy = buf.readDouble(); nvz = buf.readDouble();
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
                ByteBufUtils.writeTag(buf, customData);
                break;
            case UPDATE:
                buf.writeDouble(nvx); buf.writeDouble(nvy); buf.writeDouble(nvz);
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
                net.minecraftforge.fml.common.FMLCommonHandler.instance()
                        .getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                            ClientDanmakuCache cache = ClientDanmakuCache.INSTANCE;
                            if (cache == null) return;
                            switch (message.op) {
                                case SPAWN:
                                    ResourceLocation tex = null;
                                    if (message.texture != null && !message.texture.isEmpty()) {
                                        tex = new ResourceLocation(message.texture);
                                    }
                                    cache.spawnBullet(message.id,
                                            new Vec3d(message.x, message.y, message.z),
                                            new Vec3d(message.vx, message.vy, message.vz),
                                            message.life, message.damage,
                                            tex, message.customData);
                                    break;
                                case UPDATE:
                                    cache.updateBulletVelocity(message.id,
                                            new Vec3d(message.nvx, message.nvy, message.nvz));
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