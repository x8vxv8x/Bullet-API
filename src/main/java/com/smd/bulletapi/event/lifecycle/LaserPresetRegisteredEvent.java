package com.smd.bulletapi.event.lifecycle;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.preset.LaserPreset;
import net.minecraftforge.fml.common.eventhandler.Event;

@PublicApi
public class LaserPresetRegisteredEvent extends Event {
    private final String id;
    private final LaserPreset preset;

    public LaserPresetRegisteredEvent(String id, LaserPreset preset) {
        this.id = id;
        this.preset = preset;
    }

    public String getId() {
        return id;
    }

    public LaserPreset getPreset() {
        return preset;
    }
}
