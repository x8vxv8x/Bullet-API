package com.smd.bulletapi.common.data;

/**
 * DataPayload 辅助方法，统一空值和差量处理。
 */
public final class Payloads {
    private Payloads() {}

    public static DataPayload empty() {
        return new DataPayload();
    }

    public static DataPayload copyOf(DataPayload payload) {
        return payload == null ? new DataPayload() : payload.copy();
    }

    public static boolean isEmpty(DataPayload payload) {
        return payload == null || payload.isEmpty();
    }

    public static DataPayload diff(DataPayload actual, DataPayload base) {
        if (isEmpty(actual)) {
            return new DataPayload();
        }
        if (isEmpty(base)) {
            return actual.copy();
        }

        DataPayload diff = new DataPayload();
        for (String key : actual.getKeySet()) {
            Object actualValue = actual.getRawValue(key);
            Object baseValue = base.getRawValue(key);
            if (actualValue == null ? baseValue != null : !actualValue.equals(baseValue)) {
                copyValue(diff, key, actual);
            }
        }
        return diff;
    }

    public static DataPayload merge(DataPayload base, DataPayload diff) {
        DataPayload merged = copyOf(base);
        if (isEmpty(diff)) {
            return merged;
        }
        for (String key : diff.getKeySet()) {
            copyValue(merged, key, diff);
        }
        return merged;
    }

    private static void copyValue(DataPayload target, String key, DataPayload source) {
        DataPayload.ValueType type = source.getType(key);
        if (type == null) {
            return;
        }
        switch (type) {
            case STRING:
                target.setString(key, source.getString(key));
                break;
            case INT:
                target.setInteger(key, source.getInteger(key));
                break;
            case FLOAT:
                target.setFloat(key, source.getFloat(key));
                break;
            case BOOLEAN:
                target.setBoolean(key, source.getBoolean(key));
                break;
            case PAYLOAD:
                target.setTag(key, source.getCompoundTag(key));
                break;
            default:
                throw new IllegalStateException("Unsupported payload value type: " + type);
        }
    }
}
