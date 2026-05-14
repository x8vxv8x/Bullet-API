package com.smd.bulletapi.common;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.api.builder.BulletBuilder;
import com.smd.bulletapi.api.builder.LaserBuilder;
import com.smd.bulletapi.api.preset.BulletPreset;
import com.smd.bulletapi.api.preset.BulletPresetRegistry;
import com.smd.bulletapi.api.preset.LaserPreset;
import com.smd.bulletapi.api.preset.LaserPresetRegistry;
import com.smd.bulletapi.common.summon.SummonDefinition;
import com.smd.bulletapi.common.summon.SummonRegistry;
import com.smd.bulletapi.server.Bullet;
import com.smd.bulletapi.server.Laser;
import com.smd.bulletapi.server.summon.SummonBullet;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

@InternalApi
public final class RenderDataRefs {
    private RenderDataRefs() {}

    public static final int FLAG_TEXTURE = 1;
    public static final int FLAG_COLOR = 1 << 1;
    public static final int FLAG_SIZE = 1 << 2;
    public static final int FLAG_RENDERER = 1 << 3;
    public static final int FLAG_CUSTOM_DATA = 1 << 4;
    public static final int FLAG_THICKNESS = 1 << 5;

    public static BulletRenderData bulletFromRuntime(Bullet bullet) {
        return bullet == null ? null : new BulletRenderData(
                bullet.getTexture(),
                bullet.getColor(),
                bullet.getSize(),
                bullet.getRendererType(),
                copyTag(bullet.getCustomData())
        );
    }

    public static BulletRenderData bulletFromPreset(String presetId) {
        if (presetId == null || !BulletPresetRegistry.has(presetId)) {
            return null;
        }
        BulletPreset preset = BulletPresetRegistry.get(presetId);
        if (preset == null) {
            return null;
        }
        BulletBuilder builder = new BulletBuilder(null);
        preset.apply(builder);
        return new BulletRenderData(
                builder.getTexture(),
                builder.getColor(),
                builder.getSize(),
                builder.getRendererType(),
                builder.getCustomData()
        );
    }

    public static LaserRenderData laserFromRuntime(Laser laser) {
        return laser == null ? null : new LaserRenderData(
                laser.getThickness(),
                laser.getColor(),
                laser.getRendererType(),
                copyTag(laser.getCustomData())
        );
    }

    public static LaserRenderData laserFromPreset(String presetId) {
        if (presetId == null || !LaserPresetRegistry.has(presetId)) {
            return null;
        }
        LaserPreset preset = LaserPresetRegistry.get(presetId);
        if (preset == null) {
            return null;
        }
        LaserBuilder builder = new LaserBuilder(null);
        preset.apply(builder);
        return new LaserRenderData(
                builder.getThickness(),
                builder.getColor(),
                builder.getRendererType(),
                builder.getCustomData()
        );
    }

    public static BulletRenderData summonFromRuntime(SummonBullet summon) {
        return summon == null ? null : new BulletRenderData(
                summon.getTexture(),
                summon.getColor(),
                summon.getSize(),
                summon.getRendererType(),
                copyTag(summon.getCustomData())
        );
    }

    public static BulletRenderData summonFromDefinition(String definitionId) {
        if (definitionId == null || !SummonRegistry.has(definitionId)) {
            return null;
        }
        SummonDefinition definition = SummonRegistry.get(definitionId);
        if (definition == null) {
            return null;
        }
        return new BulletRenderData(
                definition.getTexture(),
                definition.getColor(),
                definition.getSize(),
                definition.getRendererType(),
                copyTag(definition.getCustomData())
        );
    }

    public static int bulletDiffFlags(BulletRenderData actual, BulletRenderData base) {
        return bulletDiffFlags(actual, base, diff(actual == null ? null : actual.customData, base == null ? null : base.customData));
    }

    public static int bulletDiffFlags(BulletRenderData actual, BulletRenderData base, NBTTagCompound customDataDiff) {
        if (actual == null || base == null) {
            return FLAG_TEXTURE | FLAG_COLOR | FLAG_SIZE | FLAG_RENDERER | FLAG_CUSTOM_DATA;
        }
        int flags = 0;
        if (!equalsNullable(actual.texture, base.texture)) {
            flags |= FLAG_TEXTURE;
        }
        if (actual.color != base.color) {
            flags |= FLAG_COLOR;
        }
        if (Float.compare(actual.size, base.size) != 0) {
            flags |= FLAG_SIZE;
        }
        if (!equalsNullable(actual.rendererType, base.rendererType)) {
            flags |= FLAG_RENDERER;
        }
        if (!isEmpty(customDataDiff)) {
            flags |= FLAG_CUSTOM_DATA;
        }
        return flags;
    }

    public static int laserDiffFlags(LaserRenderData actual, LaserRenderData base) {
        return laserDiffFlags(actual, base, diff(actual == null ? null : actual.customData, base == null ? null : base.customData));
    }

    public static int laserDiffFlags(LaserRenderData actual, LaserRenderData base, NBTTagCompound customDataDiff) {
        if (actual == null || base == null) {
            return FLAG_THICKNESS | FLAG_COLOR | FLAG_RENDERER | FLAG_CUSTOM_DATA;
        }
        int flags = 0;
        if (Float.compare(actual.thickness, base.thickness) != 0) {
            flags |= FLAG_THICKNESS;
        }
        if (actual.color != base.color) {
            flags |= FLAG_COLOR;
        }
        if (!equalsNullable(actual.rendererType, base.rendererType)) {
            flags |= FLAG_RENDERER;
        }
        if (!isEmpty(customDataDiff)) {
            flags |= FLAG_CUSTOM_DATA;
        }
        return flags;
    }

    public static NBTTagCompound diff(NBTTagCompound actual, NBTTagCompound base) {
        if (actual == null || actual.isEmpty()) {
            return new NBTTagCompound();
        }
        if (base == null || base.isEmpty()) {
            return actual.copy();
        }
        NBTTagCompound diff = new NBTTagCompound();
        for (String key : actual.getKeySet()) {
            NBTBase actualTag = actual.getTag(key);
            NBTBase baseTag = base.getTag(key);
            if (!actualTag.equals(baseTag)) {
                diff.setTag(key, actualTag.copy());
            }
        }
        return diff;
    }

    public static NBTTagCompound merge(NBTTagCompound base, NBTTagCompound diff) {
        NBTTagCompound merged = copyTag(base);
        if (diff == null || diff.isEmpty()) {
            return merged;
        }
        for (String key : diff.getKeySet()) {
            merged.setTag(key, diff.getTag(key).copy());
        }
        return merged;
    }

    public static boolean isEmpty(NBTTagCompound tag) {
        return tag == null || tag.isEmpty();
    }

    public static NBTTagCompound copyTag(NBTTagCompound tag) {
        return tag == null ? new NBTTagCompound() : tag.copy();
    }

    private static boolean equalsNullable(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    public static final class BulletRenderData {
        public final String texture;
        public final int color;
        public final float size;
        public final String rendererType;
        public final NBTTagCompound customData;

        public BulletRenderData(String texture, int color, float size, String rendererType, NBTTagCompound customData) {
            this.texture = texture;
            this.color = color;
            this.size = size;
            this.rendererType = rendererType;
            this.customData = copyTag(customData);
        }
    }

    public static final class LaserRenderData {
        public final float thickness;
        public final int color;
        public final String rendererType;
        public final NBTTagCompound customData;

        public LaserRenderData(float thickness, int color, String rendererType, NBTTagCompound customData) {
            this.thickness = thickness;
            this.color = color;
            this.rendererType = rendererType;
            this.customData = copyTag(customData);
        }
    }
}
