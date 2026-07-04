package com.smd.bulletapi.common.data;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 轻量扁平参数载荷，替代渲染/运行时热路径上的 NBTTagCompound。
 */
public final class DataPayload {
    private final Map<String, Entry> entries = new HashMap<>();

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean hasKey(String key) {
        return key != null && entries.containsKey(key);
    }

    public Set<String> getKeySet() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    public void removeTag(String key) {
        if (key != null) {
            entries.remove(key);
        }
    }

    public void setString(String key, String value) {
        put(key, ValueType.STRING, value == null ? "" : value);
    }

    public void setInteger(String key, int value) {
        put(key, ValueType.INT, value);
    }

    public void setFloat(String key, float value) {
        put(key, ValueType.FLOAT, value);
    }

    public void setBoolean(String key, boolean value) {
        put(key, ValueType.BOOLEAN, value);
    }

    public void setTag(String key, DataPayload value) {
        put(key, ValueType.PAYLOAD, value == null ? new DataPayload() : value.copy());
    }

    public String getString(String key) {
        Entry entry = entries.get(key);
        return entry != null && entry.type == ValueType.STRING ? (String) entry.value : "";
    }

    public int getInteger(String key) {
        Entry entry = entries.get(key);
        return entry != null && entry.type == ValueType.INT ? (Integer) entry.value : 0;
    }

    public float getFloat(String key) {
        Entry entry = entries.get(key);
        return entry != null && entry.type == ValueType.FLOAT ? (Float) entry.value : 0.0f;
    }

    public boolean getBoolean(String key) {
        Entry entry = entries.get(key);
        return entry != null && entry.type == ValueType.BOOLEAN && (Boolean) entry.value;
    }

    public DataPayload getCompoundTag(String key) {
        Entry entry = entries.get(key);
        if (entry == null || entry.type != ValueType.PAYLOAD) {
            return new DataPayload();
        }
        return ((DataPayload) entry.value).copy();
    }

    public ValueType getType(String key) {
        Entry entry = entries.get(key);
        return entry == null ? null : entry.type;
    }

    public Object getRawValue(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.type == ValueType.PAYLOAD) {
            return ((DataPayload) entry.value).copy();
        }
        return entry.value;
    }

    public DataPayload copy() {
        DataPayload copy = new DataPayload();
        for (Map.Entry<String, Entry> entry : entries.entrySet()) {
            copy.entries.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    public void writeTo(ByteBuf buf) {
        buf.writeInt(entries.size());
        for (Map.Entry<String, Entry> entry : entries.entrySet()) {
            ByteBufUtils.writeUTF8String(buf, entry.getKey());
            entry.getValue().writeTo(buf);
        }
    }

    public static DataPayload readFrom(ByteBuf buf) {
        DataPayload payload = new DataPayload();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            String key = ByteBufUtils.readUTF8String(buf);
            payload.entries.put(key, Entry.readFrom(buf));
        }
        return payload;
    }

    private void put(String key, ValueType type, Object value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Payload key must not be empty");
        }
        entries.put(key, new Entry(type, value));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DataPayload)) {
            return false;
        }
        DataPayload other = (DataPayload) obj;
        return entries.equals(other.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    public enum ValueType {
        STRING,
        INT,
        FLOAT,
        BOOLEAN,
        PAYLOAD;

        private static final ValueType[] VALUES = values();

        private static ValueType fromOrdinal(int ordinal) {
            if (ordinal < 0 || ordinal >= VALUES.length) {
                throw new IllegalStateException("Invalid payload value type: " + ordinal);
            }
            return VALUES[ordinal];
        }
    }

    private static final class Entry {
        private final ValueType type;
        private final Object value;

        private Entry(ValueType type, Object value) {
            this.type = Objects.requireNonNull(type, "type");
            this.value = value;
        }

        private Entry copy() {
            if (type == ValueType.PAYLOAD) {
                return new Entry(type, ((DataPayload) value).copy());
            }
            return new Entry(type, value);
        }

        private void writeTo(ByteBuf buf) {
            buf.writeByte(type.ordinal());
            switch (type) {
                case STRING:
                    ByteBufUtils.writeUTF8String(buf, (String) value);
                    break;
                case INT:
                    buf.writeInt((Integer) value);
                    break;
                case FLOAT:
                    buf.writeFloat((Float) value);
                    break;
                case BOOLEAN:
                    buf.writeBoolean((Boolean) value);
                    break;
                case PAYLOAD:
                    ((DataPayload) value).writeTo(buf);
                    break;
                default:
                    throw new IllegalStateException("Unsupported payload value type: " + type);
            }
        }

        private static Entry readFrom(ByteBuf buf) {
            ValueType type = ValueType.fromOrdinal(buf.readByte());
            switch (type) {
                case STRING:
                    return new Entry(type, ByteBufUtils.readUTF8String(buf));
                case INT:
                    return new Entry(type, buf.readInt());
                case FLOAT:
                    return new Entry(type, buf.readFloat());
                case BOOLEAN:
                    return new Entry(type, buf.readBoolean());
                case PAYLOAD:
                    return new Entry(type, DataPayload.readFrom(buf));
                default:
                    throw new IllegalStateException("Unsupported payload value type: " + type);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Entry)) {
                return false;
            }
            Entry other = (Entry) obj;
            return type == other.type && Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, value);
        }
    }
}
