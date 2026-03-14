package com.smd.bulletapi.event.lifecycle;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.preset.BulletPreset;
import net.minecraftforge.fml.common.eventhandler.Event;

@PublicApi
public class BulletPresetRegisteredEvent extends Event {
    private final String id;
    private final BulletPreset preset;

    public BulletPresetRegisteredEvent(String id, BulletPreset preset) {
        this.id = id;
        this.preset = preset;
    }

    public String getId() {
        return id;
    }

    public BulletPreset getPreset() {
        return preset;
    }
}
