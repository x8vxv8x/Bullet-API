package com.smd.bulletapi.common.runtime.state;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.common.RenderStateData;
import com.smd.bulletapi.common.data.DataPayload;

@InternalApi
public final class PayloadState {
    private DataPayload customData;

    public PayloadState(DataPayload customData) {
        this.customData = customData == null ? new DataPayload() : customData;
    }

    public DataPayload getCustomData() {
        return customData;
    }

    public void setCustomData(DataPayload customData) {
        this.customData = customData == null ? new DataPayload() : customData;
    }

    public String getRenderState() {
        return RenderStateData.getRenderState(customData);
    }

    public void setRenderState(String renderState) {
        if (customData == null) {
            customData = new DataPayload();
        }
        RenderStateData.setRenderState(customData, renderState);
    }
}
