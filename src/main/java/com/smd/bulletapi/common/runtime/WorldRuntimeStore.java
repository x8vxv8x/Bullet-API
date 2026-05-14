package com.smd.bulletapi.common.runtime;

import com.smd.bulletapi.api.annotation.InternalApi;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一管理 world -> id -> runtime object 的存储和常见只读查询。
 */
@InternalApi
public class WorldRuntimeStore<T extends RuntimeObject> {
    private final Map<World, Map<Integer, T>> worldEntries = new HashMap<>();

    public Map<Integer, T> getOrCreateWorldEntries(World world) {
        return worldEntries.computeIfAbsent(world, ignored -> new HashMap<>());
    }

    public Map<Integer, T> getWorldEntries(World world) {
        return worldEntries.get(world);
    }

    public void put(World world, T runtime) {
        getOrCreateWorldEntries(world).put(runtime.getId(), runtime);
    }

    public T remove(World world, int id) {
        Map<Integer, T> entries = worldEntries.get(world);
        return entries == null ? null : entries.remove(id);
    }

    public T get(World world, int id) {
        Map<Integer, T> entries = worldEntries.get(world);
        return entries == null ? null : entries.get(id);
    }

    public T getLive(World world, int id) {
        T runtime = get(world, id);
        return runtime == null || runtime.isDead() ? null : runtime;
    }

    public boolean hasLive(World world, int id) {
        return getLive(world, id) != null;
    }

    public boolean isCurrent(World world, int id, T runtime) {
        Map<Integer, T> entries = worldEntries.get(world);
        return entries != null && entries.get(id) == runtime;
    }

    public int countLive(World world) {
        Map<Integer, T> entries = worldEntries.get(world);
        if (entries == null) {
            return 0;
        }

        int count = 0;
        for (T runtime : entries.values()) {
            if (!runtime.isDead()) {
                count++;
            }
        }
        return count;
    }

    public List<Integer> getLiveIds(World world) {
        Map<Integer, T> entries = worldEntries.get(world);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> ids = new ArrayList<>();
        for (Map.Entry<Integer, T> entry : entries.entrySet()) {
            if (!entry.getValue().isDead()) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }

    public <S> List<S> getLiveSnapshots(World world, RuntimeSnapshotFactory<? super T, S> snapshotFactory) {
        Map<Integer, T> entries = worldEntries.get(world);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<S> snapshots = new ArrayList<>();
        for (T runtime : entries.values()) {
            if (!runtime.isDead()) {
                snapshots.add(snapshotFactory.create(runtime));
            }
        }
        return snapshots;
    }

    public <S> S getLiveSnapshot(World world, int id, RuntimeSnapshotFactory<? super T, S> snapshotFactory) {
        T runtime = getLive(world, id);
        return runtime == null ? null : snapshotFactory.create(runtime);
    }

    public List<T> getEntriesSnapshot(World world) {
        Map<Integer, T> entries = worldEntries.get(world);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(entries.values());
    }

    public Map<Integer, T> removeWorld(World world) {
        return worldEntries.remove(world);
    }
}
