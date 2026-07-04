package com.smd.bulletapi.common.runtime.state;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.common.data.DataPayload;

@InternalApi
public final class SpriteVisualState {
    private String texture;
    private int color;
    private float size;
    private String rendererType;
    private final PayloadState payload;

    public SpriteVisualState(String texture, int color, float size, String rendererType, DataPayload customData) {
        this.texture = texture;
        this.color = color;
        this.size = size;
        this.rendererType = rendererType;
        this.payload = new PayloadState(customData);
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture == null || texture.isEmpty() ? null : texture;
    }

    public int getColor() {
        return color;
    }

    public float getSize() {
        return size;
    }

    public String getRendererType() {
        return rendererType;
    }

    public void setRendererType(String rendererType) {
        this.rendererType = rendererType == null || rendererType.isEmpty() ? null : rendererType;
    }

    public DataPayload getCustomData() {
        return payload.getCustomData();
    }

    public void setCustomData(DataPayload customData) {
        payload.setCustomData(customData);
    }

    public String getRenderState() {
        return payload.getRenderState();
    }

    public void setRenderState(String renderState) {
        payload.setRenderState(renderState);
    }
}
