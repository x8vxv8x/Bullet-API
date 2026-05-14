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

import javax.annotation.Nullable;

/**
 * 激光同步数据包（服务端 -> 客户端）
 *
 * @author smd
 */
@InternalApi
public class SPacketLaser implements IMessage {

    /** 更新标志：起始点变更 */
    public static final int FLAG_START = 1;
    /** 更新标志：方向变更 */
    public static final int FLAG_DIRECTION = 1 << 1;
    /** 更新标志：长度变更 */
    public static final int FLAG_LENGTH = 1 << 2;

    /** 数据包操作类型 */
    public enum Operation {
        /** 生成新激光实体（包含全部基础数据与渲染数据） */
        SPAWN,
        /** 增量更新激光属性（仅包含变更的坐标/方向/长度） */
        UPDATE,
        /** 移除激光实体 */
        REMOVE
    }

    /** 当前操作 */
    private Operation op;
    /** 激光实体 ID */
    private int id;
    /** 对应游戏刻 */
    private long tick;
    /** 起始坐标（UPDATE / SPAWN 可能为 null） */
    @Nullable
    private Vec3d start;
    /** 方向向量（UPDATE / SPAWN 可能为 null） */
    @Nullable
    private Vec3d direction;
    /** 长度（UPDATE / SPAWN 可能为 null） */
    @Nullable
    private Double length;
    /** 粗细（SPAWN 时有效） */
    private float thickness;
    /** 颜色（SPAWN 时有效） */
    private int color;
    /** 渲染器类型（SPAWN 时有效） */
    @Nullable
    private String rendererType;
    /** 自定义 NBT 数据（SPAWN 时有效） */
    @Nullable
    private NBTTagCompound customData;
    /** 是否引用预设 */
    private boolean usePresetRef;
    /** 预设 ID */
    @Nullable
    private String presetId;
    /** 渲染数据差异标志位（与 RenderDataRefs.FLAG_* 对应） */
    private int flags;

    public SPacketLaser() {}

    private SPacketLaser(Operation op, int id) {
        this.op = op;
        this.id = id;
    }

    /**
     * 生成一个完整的激光实体创建包
     *
     * @param laser 服务端激光实例
     * @param tick  当前游戏刻
     * @return 数据包
     */
    public static SPacketLaser createSpawn(Laser laser, long tick) {
        SPacketLaser p = new SPacketLaser(Operation.SPAWN, laser.getId());
        p.tick = tick;
        p.start = laser.getStart();
        p.direction = laser.getDirection();
        p.length = laser.getCurrentLength();
        applyRenderData(p, laser);
        return p;
    }

    /**
     * 生成增量更新包（仅包含变更的字段）
     *
     * @param id     激光实体 ID
     * @param tick   当前游戏刻
     * @param flags  变更标志位（FLAG_START | FLAG_DIRECTION | FLAG_LENGTH）
     * @param start  新的起始点（null 表示不变）
     * @param dir    新的方向（null 表示不变）
     * @param length 新的长度（null 表示不变）
     * @return 数据包
     */
    public static SPacketLaser createUpdate(int id, long tick, int flags,
                                            @Nullable Vec3d start, @Nullable Vec3d dir, @Nullable Double length) {
        SPacketLaser p = new SPacketLaser(Operation.UPDATE, id);
        p.tick = tick;
        p.flags = flags;
        if ((flags & FLAG_START) != 0) {
            p.start = start;
        }
        if ((flags & FLAG_DIRECTION) != 0) {
            p.direction = dir;
        }
        if ((flags & FLAG_LENGTH) != 0) {
            p.length = length;
        }
        return p;
    }

    /**
     * 生成移除包
     *
     * @param id 激光实体 ID
     * @return 数据包
     */
    public static SPacketLaser createRemove(int id) {
        return new SPacketLaser(Operation.REMOVE, id);
    }

    /**
     * 根据激光的预设引用决定渲染数据传输策略（全量/差异）
     */
    private static void applyRenderData(SPacketLaser p, Laser laser) {
        RenderDataRefs.LaserRenderData actual = RenderDataRefs.laserFromRuntime(laser);
        String presetId = laser.getRenderPresetId();
        RenderDataRefs.LaserRenderData base = (presetId != null) ? RenderDataRefs.laserFromPreset(presetId) : null;

        if (base != null) {
            NBTTagCompound customDiff = RenderDataRefs.diff(actual.customData, base.customData);
            p.usePresetRef = true;
            p.presetId = presetId;
            p.flags = RenderDataRefs.laserDiffFlags(actual, base, customDiff);
            if ((p.flags & RenderDataRefs.FLAG_THICKNESS) != 0) {
                p.thickness = actual.thickness;
            }
            if ((p.flags & RenderDataRefs.FLAG_COLOR) != 0) {
                p.color = actual.color;
            }
            if ((p.flags & RenderDataRefs.FLAG_RENDERER) != 0) {
                p.rendererType = actual.rendererType;
            }
            if ((p.flags & RenderDataRefs.FLAG_CUSTOM_DATA) != 0) {
                p.customData = customDiff;
            }
        } else {
            p.usePresetRef = false;
            p.thickness = actual.thickness;
            p.color = actual.color;
            p.rendererType = actual.rendererType;
            p.customData = actual.customData;
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int opIndex = buf.readInt();
        if (opIndex < 0 || opIndex >= Operation.values().length) {
            throw new IllegalStateException("Invalid laser operation index: " + opIndex);
        }
        op = Operation.values()[opIndex];
        id = buf.readInt();

        switch (op) {
            case SPAWN:
                readSpawnData(buf);
                break;
            case UPDATE:
                readUpdateData(buf);
                break;
            case REMOVE:
            default:
                break;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(op.ordinal());
        buf.writeInt(id);

        switch (op) {
            case SPAWN:
                writeSpawnData(buf);
                break;
            case UPDATE:
                writeUpdateData(buf);
                break;
            case REMOVE:
            default:
                break;
        }
    }

    private void writeSpawnData(ByteBuf buf) {
        buf.writeLong(tick);
        writeVec3d(buf, start);
        writeVec3d(buf, direction);
        buf.writeDouble(length != null ? length : 0.0);
        buf.writeBoolean(usePresetRef);
        if (usePresetRef) {
            ByteBufUtils.writeUTF8String(buf, presetId != null ? presetId : "");
            buf.writeInt(flags);
            writeRenderFlagsAndData(buf);
        } else {
            buf.writeFloat(thickness);
            buf.writeInt(color);
            ByteBufUtils.writeUTF8String(buf, rendererType != null ? rendererType : "");
            ByteBufUtils.writeTag(buf, customData);
        }
    }

    private void readSpawnData(ByteBuf buf) {
        tick = buf.readLong();
        start = readVec3d(buf);
        direction = readVec3d(buf);
        length = buf.readDouble();
        usePresetRef = buf.readBoolean();
        if (usePresetRef) {
            presetId = ByteBufUtils.readUTF8String(buf);
            flags = buf.readInt();
            readRenderFlagsAndData(buf);
        } else {
            thickness = buf.readFloat();
            color = buf.readInt();
            rendererType = ByteBufUtils.readUTF8String(buf);
            customData = ByteBufUtils.readTag(buf);
        }
    }

    private void writeUpdateData(ByteBuf buf) {
        buf.writeLong(tick);
        buf.writeInt(flags);
        if ((flags & FLAG_START) != 0) {
            writeVec3d(buf, start);
        }
        if ((flags & FLAG_DIRECTION) != 0) {
            writeVec3d(buf, direction);
        }
        if ((flags & FLAG_LENGTH) != 0 && length != null) {
            buf.writeDouble(length);
        }
    }

    private void readUpdateData(ByteBuf buf) {
        tick = buf.readLong();
        flags = buf.readInt();
        if ((flags & FLAG_START) != 0) {
            start = readVec3d(buf);
        }
        if ((flags & FLAG_DIRECTION) != 0) {
            direction = readVec3d(buf);
        }
        if ((flags & FLAG_LENGTH) != 0) {
            length = buf.readDouble();
        }
    }

    /**
     * 根据 flags 写入渲染差异数据
     */
    private void writeRenderFlagsAndData(ByteBuf buf) {
        if ((flags & RenderDataRefs.FLAG_THICKNESS) != 0) {
            buf.writeFloat(thickness);
        }
        if ((flags & RenderDataRefs.FLAG_COLOR) != 0) {
            buf.writeInt(color);
        }
        if ((flags & RenderDataRefs.FLAG_RENDERER) != 0) {
            ByteBufUtils.writeUTF8String(buf, rendererType != null ? rendererType : "");
        }
        if ((flags & RenderDataRefs.FLAG_CUSTOM_DATA) != 0) {
            ByteBufUtils.writeTag(buf, customData);
        }
    }

    /**
     * 根据 flags 读取渲染差异数据
     */
    private void readRenderFlagsAndData(ByteBuf buf) {
        if ((flags & RenderDataRefs.FLAG_THICKNESS) != 0) {
            thickness = buf.readFloat();
        }
        if ((flags & RenderDataRefs.FLAG_COLOR) != 0) {
            color = buf.readInt();
        }
        if ((flags & RenderDataRefs.FLAG_RENDERER) != 0) {
            rendererType = ByteBufUtils.readUTF8String(buf);
        }
        if ((flags & RenderDataRefs.FLAG_CUSTOM_DATA) != 0) {
            customData = ByteBufUtils.readTag(buf);
        }
    }

    /** 写入三维向量 */
    private static void writeVec3d(ByteBuf buf, @Nullable Vec3d vec) {
        if (vec == null) {
            buf.writeDouble(0.0);
            buf.writeDouble(0.0);
            buf.writeDouble(0.0);
        } else {
            buf.writeDouble(vec.x);
            buf.writeDouble(vec.y);
            buf.writeDouble(vec.z);
        }
    }

    /** 读取三维向量，永远非 null */
    private static Vec3d readVec3d(ByteBuf buf) {
        return new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static class Handler implements IMessageHandler<SPacketLaser, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(SPacketLaser message, MessageContext ctx) {
            PacketHandler.runOnClientThread(ctx, () -> processLaserPacket(message));
            return null;
        }

        private static void processLaserPacket(SPacketLaser msg) {
            ClientLaserCache cache = ClientLaserCache.INSTANCE;
            if (cache == null) {
                return;
            }
            switch (msg.op) {
                case SPAWN:
                    handleSpawn(msg, cache);
                    break;
                case UPDATE:
                    handleUpdate(msg, cache);
                    break;
                case REMOVE:
                    cache.removeLaser(msg.id);
                    break;
                default:
                    break;
            }
        }

        private static void handleSpawn(SPacketLaser msg, ClientLaserCache cache) {
            float thickness = msg.thickness;
            int color = msg.color;
            String rendererType = msg.rendererType;
            NBTTagCompound customData = msg.customData;

            if (msg.usePresetRef) {
                RenderDataRefs.LaserRenderData base = RenderDataRefs.laserFromPreset(msg.presetId);
                if (base != null) {
                    if ((msg.flags & RenderDataRefs.FLAG_THICKNESS) == 0) {
                        thickness = base.thickness;
                    }
                    if ((msg.flags & RenderDataRefs.FLAG_COLOR) == 0) {
                        color = base.color;
                    }
                    if ((msg.flags & RenderDataRefs.FLAG_RENDERER) == 0) {
                        rendererType = base.rendererType;
                    }
                    customData = (msg.flags & RenderDataRefs.FLAG_CUSTOM_DATA) != 0
                            ? RenderDataRefs.merge(base.customData, msg.customData)
                            : base.customData;
                }
            }
            cache.spawnLaser(msg.id, msg.tick, msg.start, msg.direction, msg.length,
                    thickness, color, rendererType, customData);
        }

        private static void handleUpdate(SPacketLaser msg, ClientLaserCache cache) {
            cache.updateLaser(
                    msg.id, msg.tick, msg.flags,
                    (msg.flags & FLAG_START) != 0 ? msg.start : null,
                    (msg.flags & FLAG_DIRECTION) != 0 ? msg.direction : null,
                    (msg.flags & FLAG_LENGTH) != 0 ? msg.length : null
            );
        }
    }
}