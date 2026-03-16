package com.smd.bulletapi.network;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.client.ClientDanmakuCache;
import com.smd.bulletapi.client.ClientSummonCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@InternalApi
public class SPacketBulletVisual implements IMessage {
    public static final int FLAG_TEXTURE = 1;
    public static final int FLAG_RENDERER = 1 << 1;
    public static final int FLAG_RENDER_STATE = 1 << 2;

    public enum TargetType {
        BULLET, SUMMON
    }

    private TargetType targetType;
    private int id;
    private int flags;
    private String texture;
    private String rendererType;
    private String renderState;

    public SPacketBulletVisual() {}

    private SPacketBulletVisual(TargetType targetType, int id, int flags) {
        this.targetType = targetType;
        this.id = id;
        this.flags = flags;
    }

    public static SPacketBulletVisual createBullet(int id, int flags, String texture, String rendererType, String renderState) {
        return create(TargetType.BULLET, id, flags, texture, rendererType, renderState);
    }

    public static SPacketBulletVisual createSummon(int id, int flags, String texture, String rendererType, String renderState) {
        return create(TargetType.SUMMON, id, flags, texture, rendererType, renderState);
    }

    private static SPacketBulletVisual create(TargetType targetType, int id, int flags, String texture, String rendererType, String renderState) {
        SPacketBulletVisual packet = new SPacketBulletVisual(targetType, id, flags);
        packet.texture = texture;
        packet.rendererType = rendererType;
        packet.renderState = renderState;
        return packet;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        targetType = TargetType.values()[buf.readByte()];
        id = buf.readInt();
        flags = buf.readInt();
        if ((flags & FLAG_TEXTURE) != 0) {
            texture = emptyToNull(ByteBufUtils.readUTF8String(buf));
        }
        if ((flags & FLAG_RENDERER) != 0) {
            rendererType = emptyToNull(ByteBufUtils.readUTF8String(buf));
        }
        if ((flags & FLAG_RENDER_STATE) != 0) {
            renderState = emptyToNull(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(targetType.ordinal());
        buf.writeInt(id);
        buf.writeInt(flags);
        if ((flags & FLAG_TEXTURE) != 0) {
            ByteBufUtils.writeUTF8String(buf, nullToEmpty(texture));
        }
        if ((flags & FLAG_RENDERER) != 0) {
            ByteBufUtils.writeUTF8String(buf, nullToEmpty(rendererType));
        }
        if ((flags & FLAG_RENDER_STATE) != 0) {
            ByteBufUtils.writeUTF8String(buf, nullToEmpty(renderState));
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    public static class Handler implements IMessageHandler<SPacketBulletVisual, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(SPacketBulletVisual message, MessageContext ctx) {
            PacketHandler.runOnClientThread(ctx, () -> {
                ResourceLocation textureLocation = null;
                if ((message.flags & FLAG_TEXTURE) != 0 && message.texture != null) {
                    textureLocation = new ResourceLocation(message.texture);
                }
                switch (message.targetType) {
                    case BULLET:
                        if (ClientDanmakuCache.INSTANCE != null) {
                            ClientDanmakuCache.INSTANCE.updateVisual(
                                    message.id,
                                    message.flags,
                                    textureLocation,
                                    message.rendererType,
                                    message.renderState
                            );
                        }
                        break;
                    case SUMMON:
                        if (ClientSummonCache.INSTANCE != null) {
                            ClientSummonCache.INSTANCE.updateVisual(
                                    message.id,
                                    message.flags,
                                    textureLocation,
                                    message.rendererType,
                                    message.renderState
                            );
                        }
                        break;
                }
            });
            return null;
        }
    }
}
