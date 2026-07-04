package com.smd.bulletapi.common.runtime.state;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.common.data.DataPayload;

@InternalApi
public final class LaserVisualState {
    private final float thickness;
    private int color;
    private String rendererType;
    private final PayloadState payload;

    public LaserVisualState(float thickness, int color, String rendererType, DataPayload customData) {
        this.thickness = thickness;
        this.color = color;
        this.rendererType = rendererType;
        this.payload = new PayloadState(customData);
    }

    public float getThickness() {
        return thickness;
    }

    public int getColor() {
        return color;
    }

    public String getRendererType() {
        return rendererType;
    }

    public DataPayload getCustomData() {
        return payload.getCustomData();
    }
}
