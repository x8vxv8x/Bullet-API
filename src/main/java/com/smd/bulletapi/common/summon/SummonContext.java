package com.smd.bulletapi.common.summon;

import com.smd.bulletapi.api.SummonApi;
import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.runtime.ISummonActor;
import com.smd.bulletapi.common.summon.SummonTargetSource;
import com.smd.bulletapi.server.summon.SummonBullet;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@PublicApi
public class SummonContext {
    public static final String RUNTIME_ROOT_KEY = "bulletapi_runtime";
    public static final String MODE_KEY = "mode";

    private final SummonManager manager;
    public final World world;
    public final ISummonActor summon;
    public final EntityLivingBase owner;
    public final SummonDefinition definition;
    public final long worldTick;
    private EntityLivingBase target;

    public SummonContext(SummonManager manager, World world, ISummonActor summon,
                         EntityLivingBase owner, SummonDefinition definition,
                         long worldTick, EntityLivingBase target) {
        this.manager = manager;
        this.world = world;
        this.summon = summon;
        this.owner = owner;
        this.definition = definition;
        this.worldTick = worldTick;
        this.target = target;
    }

    public EntityLivingBase getTarget() {
        return target;
    }

    public void setTarget(EntityLivingBase target) {
        this.target = target;
        summon.setTarget(target);
    }

    public void setAutoTarget(EntityLivingBase target) {
        this.target = target;
        if (summon instanceof SummonBullet) {
            ((SummonBullet) summon).setTarget(target, SummonTargetSource.AUTO);
            return;
        }
        summon.setTarget(target);
    }

    public void clearTarget() {
        this.target = null;
        if (summon instanceof SummonBullet) {
            ((SummonBullet) summon).clearTarget();
            return;
        }
        summon.setTarget(null);
    }

    public Vec3d getOwnerCenter() {
        return owner.getPositionVector().add(0, owner.getEyeHeight() * 0.7, 0);
    }

    public Vec3d getTargetCenter() {
        return target == null ? null : target.getPositionVector().add(0, target.height * 0.6, 0);
    }

    public int getOwnedSummonCount() {
        return Math.max(1, manager.getOwnedSummonCount(owner.getUniqueID()));
    }

    public String getMode() {
        NBTTagCompound runtime = getRuntimeData(false);
        if (runtime != null && runtime.hasKey(MODE_KEY)) {
            return runtime.getString(MODE_KEY);
        }
        NBTTagCompound root = getRootData(false);
        return root != null && root.hasKey(MODE_KEY) ? root.getString(MODE_KEY) : null;
    }

    public void setMode(String mode) {
        writeRuntime(runtime -> {
            if (mode == null || mode.trim().isEmpty()) {
                runtime.removeTag(MODE_KEY);
            } else {
                runtime.setString(MODE_KEY, mode);
            }
        });
    }

    public void clearMode() {
        setMode(null);
    }

    public boolean hasParam(String key) {
        if (key == null || key.isEmpty()) return false;
        NBTTagCompound runtime = getRuntimeData(false);
        if (runtime != null && runtime.hasKey(key)) {
            return true;
        }
        NBTTagCompound root = getRootData(false);
        return root != null && root.hasKey(key);
    }

    public boolean hasRuntimeParam(String key) {
        if (key == null || key.isEmpty()) return false;
        NBTTagCompound runtime = getRuntimeData(false);
        return runtime != null && runtime.hasKey(key);
    }

    public int getInt(String key, int defaultValue) {
        NBTTagCompound data = getParamContainer(key);
        return data == null ? defaultValue : data.getInteger(key);
    }

    public float getFloat(String key, float defaultValue) {
        NBTTagCompound data = getParamContainer(key);
        return data == null ? defaultValue : data.getFloat(key);
    }

    public boolean getBool(String key, boolean defaultValue) {
        NBTTagCompound data = getParamContainer(key);
        return data == null ? defaultValue : data.getBoolean(key);
    }

    public String getString(String key, String defaultValue) {
        NBTTagCompound data = getParamContainer(key);
        return data == null ? defaultValue : data.getString(key);
    }

    public void setInt(String key, int value) {
        if (key == null || key.isEmpty()) return;
        writeRuntime(runtime -> runtime.setInteger(key, value));
    }

    public void setFloat(String key, float value) {
        if (key == null || key.isEmpty()) return;
        writeRuntime(runtime -> runtime.setFloat(key, value));
    }

    public void setBool(String key, boolean value) {
        if (key == null || key.isEmpty()) return;
        writeRuntime(runtime -> runtime.setBoolean(key, value));
    }

    public void setString(String key, String value) {
        if (key == null || key.isEmpty()) return;
        writeRuntime(runtime -> {
            if (value == null) {
                runtime.removeTag(key);
            } else {
                runtime.setString(key, value);
            }
        });
    }

    public void clearParam(String key) {
        if (key == null || key.isEmpty()) return;
        writeRuntime(runtime -> runtime.removeTag(key));
    }

    public int consumeInt(String key, int defaultValue) {
        NBTTagCompound runtime = getRuntimeData(false);
        if (runtime == null || !runtime.hasKey(key)) return defaultValue;
        int value = runtime.getInteger(key);
        clearParam(key);
        return value;
    }

    public float consumeFloat(String key, float defaultValue) {
        NBTTagCompound runtime = getRuntimeData(false);
        if (runtime == null || !runtime.hasKey(key)) return defaultValue;
        float value = runtime.getFloat(key);
        clearParam(key);
        return value;
    }

    public boolean consumeBool(String key, boolean defaultValue) {
        NBTTagCompound runtime = getRuntimeData(false);
        if (runtime == null || !runtime.hasKey(key)) return defaultValue;
        boolean value = runtime.getBoolean(key);
        clearParam(key);
        return value;
    }

    public String consumeString(String key, String defaultValue) {
        NBTTagCompound runtime = getRuntimeData(false);
        if (runtime == null || !runtime.hasKey(key)) return defaultValue;
        String value = runtime.getString(key);
        clearParam(key);
        return value;
    }

    public void setTexture(String texture) {
        SummonApi.handle(world, summon.getId()).setTexture(texture);
    }

    public void setRendererType(String rendererType) {
        SummonApi.handle(world, summon.getId()).setRendererType(rendererType);
    }

    public void setRenderState(String renderState) {
        SummonApi.handle(world, summon.getId()).setRenderState(renderState);
    }

    public void setVisual(String texture, String rendererType, String renderState) {
        SummonApi.handle(world, summon.getId()).setVisual(texture, rendererType, renderState);
    }

    private NBTTagCompound getParamContainer(String key) {
        if (key == null || key.isEmpty()) return null;
        NBTTagCompound runtime = getRuntimeData(false);
        if (runtime != null && runtime.hasKey(key)) {
            return runtime;
        }
        NBTTagCompound root = getRootData(false);
        if (root != null && root.hasKey(key)) {
            return root;
        }
        return null;
    }

    private NBTTagCompound getRootData(boolean create) {
        NBTTagCompound root = summon.getCustomData();
        if (root == null && create) {
            root = new NBTTagCompound();
            summon.setCustomData(root);
        }
        return root;
    }

    private NBTTagCompound getRuntimeData(boolean create) {
        NBTTagCompound root = getRootData(create);
        if (root == null) return null;
        if (root.hasKey(RUNTIME_ROOT_KEY)) {
            return root.getCompoundTag(RUNTIME_ROOT_KEY);
        }
        if (!create) return null;
        NBTTagCompound runtime = new NBTTagCompound();
        root.setTag(RUNTIME_ROOT_KEY, runtime);
        summon.setCustomData(root);
        return runtime;
    }

    private void writeRuntime(java.util.function.Consumer<NBTTagCompound> consumer) {
        NBTTagCompound root = getRootData(true);
        NBTTagCompound runtime = getRuntimeData(true);
        consumer.accept(runtime);
        if (runtime.isEmpty()) {
            root.removeTag(RUNTIME_ROOT_KEY);
        } else {
            root.setTag(RUNTIME_ROOT_KEY, runtime);
        }
        summon.setCustomData(root);
    }
}
