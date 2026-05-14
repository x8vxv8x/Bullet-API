package com.smd.bulletapi.common.runtime.summon;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.common.runtime.WorldRuntimeStore;
import com.smd.bulletapi.server.summon.SummonBullet;
import net.minecraft.world.World;

import java.util.*;

@InternalApi
public class SummonOwnershipIndex {
    private final Map<UUID, LinkedHashSet<OwnedSummonRef>> ownerSummons = new HashMap<>();

    public void index(World world, SummonBullet summon) {
        if (world == null || summon == null) {
            return;
        }
        ownerSummons
                .computeIfAbsent(summon.getOwnerId(), ignored -> new LinkedHashSet<>())
                .add(new OwnedSummonRef(world, summon));
    }

    public void deindex(World world, SummonBullet summon) {
        if (world == null || summon == null) {
            return;
        }
        LinkedHashSet<OwnedSummonRef> refs = ownerSummons.get(summon.getOwnerId());
        if (refs == null) {
            return;
        }
        refs.remove(new OwnedSummonRef(world, summon));
        if (refs.isEmpty()) {
            ownerSummons.remove(summon.getOwnerId());
        }
    }

    public List<SummonBullet> getOwnedSummons(UUID ownerId, WorldRuntimeStore<SummonBullet> store) {
        LinkedHashSet<OwnedSummonRef> refs = getLiveOwnerRefs(ownerId, store);
        if (refs == null || refs.isEmpty()) {
            return Collections.emptyList();
        }

        List<SummonBullet> summons = new ArrayList<>(refs.size());
        for (OwnedSummonRef ref : refs) {
            summons.add(ref.summon);
        }
        return summons;
    }

    public int getOwnedSummonCount(UUID ownerId, WorldRuntimeStore<SummonBullet> store) {
        LinkedHashSet<OwnedSummonRef> refs = getLiveOwnerRefs(ownerId, store);
        return refs == null ? 0 : refs.size();
    }

    public List<OwnedSummonRef> collectOwnedRefs(UUID ownerId, WorldRuntimeStore<SummonBullet> store) {
        LinkedHashSet<OwnedSummonRef> refs = getLiveOwnerRefs(ownerId, store);
        if (refs == null || refs.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(refs);
    }

    private LinkedHashSet<OwnedSummonRef> getLiveOwnerRefs(UUID ownerId, WorldRuntimeStore<SummonBullet> store) {
        if (ownerId == null) {
            return null;
        }

        LinkedHashSet<OwnedSummonRef> refs = ownerSummons.get(ownerId);
        if (refs == null || refs.isEmpty()) {
            return refs;
        }

        Iterator<OwnedSummonRef> it = refs.iterator();
        while (it.hasNext()) {
            OwnedSummonRef ref = it.next();
            SummonBullet summon = store.get(ref.world, ref.summonId);
            if (summon == null || summon.isDead()) {
                it.remove();
            }
        }

        if (refs.isEmpty()) {
            ownerSummons.remove(ownerId);
        }
        return refs;
    }

    public static final class OwnedSummonRef {
        public final World world;
        public final int summonId;
        public final SummonBullet summon;

        private OwnedSummonRef(World world, SummonBullet summon) {
            this.world = world;
            this.summonId = summon.getId();
            this.summon = summon;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof OwnedSummonRef)) {
                return false;
            }
            OwnedSummonRef other = (OwnedSummonRef) obj;
            return this.world == other.world && this.summonId == other.summonId;
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(world);
            result = 31 * result + summonId;
            return result;
        }
    }
}
