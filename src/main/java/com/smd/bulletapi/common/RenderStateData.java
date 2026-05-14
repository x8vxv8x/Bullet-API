package com.smd.bulletapi.common;

import com.smd.bulletapi.api.annotation.InternalApi;
import net.minecraft.nbt.NBTTagCompound;

@InternalApi
public final class RenderStateData {
    public static final String KEY_RENDER_STATE = "render_state";

    private RenderStateData() {}

    public static String getRenderState(NBTTagCompound data) {
        if (data == null || !data.hasKey(KEY_RENDER_STATE)) {
            return null;
        }
        String value = data.getString(KEY_RENDER_STATE);
        return value.isEmpty() ? null : value;
    }

    public static void setRenderState(NBTTagCompound data, String renderState) {
        if (data == null) {
            return;
        }
        if (renderState == null || renderState.isEmpty()) {
            data.removeTag(KEY_RENDER_STATE);
        } else {
            data.setString(KEY_RENDER_STATE, renderState);
        }
    }

    public static String scopedKey(String baseKey, String renderState) {
        if (baseKey == null || baseKey.isEmpty() || renderState == null || renderState.isEmpty()) {
            return baseKey;
        }
        return baseKey + "_" + renderState;
    }
}
