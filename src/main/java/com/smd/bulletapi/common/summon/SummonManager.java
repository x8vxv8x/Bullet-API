package com.smd.bulletapi.common.summon;

import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.api.handle.SummonHandle;
import com.smd.bulletapi.api.snapshot.SummonSnapshot;
import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.DanmakuManager;
import com.smd.bulletapi.common.runtime.WorldRuntimeStore;
import com.smd.bulletapi.common.runtime.summon.SummonOwnershipIndex;
import com.smd.bulletapi.common.runtime.summon.SummonSnapshotFactory;
import com.smd.bulletapi.common.runtime.summon.SummonSyncService;
import com.smd.bulletapi.event.BulletCollisionEvent;
import com.smd.bulletapi.event.lifecycle.LifecycleRemoveReason;
import com.smd.bulletapi.event.lifecycle.SummonRemoveEvent;
import com.smd.bulletapi.event.lifecycle.SummonSpawnEvent;
import com.smd.bulletapi.event.lifecycle.SummonStateChangedEvent;
import com.smd.bulletapi.event.lifecycle.SummonTargetChangedEvent;
import com.smd.bulletapi.network.SPacketBulletVisual;
import com.smd.bulletapi.network.SPacketSummon;
import com.smd.bulletapi.server.summon.SummonBullet;
import com.smd.bulletapi.spi.combat.CombatRelation;
import com.smd.bulletapi.spi.combat.CombatRelationResolverRegistry;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@InternalApi
public class SummonManager {
    private static final SummonManager INSTANCE = new SummonManager();
    private static final double SNAPSHOT_POS_EPS_SQ = 0.05D * 0.05D;
    private static final double SNAPSHOT_VEL_EPS_SQ = 0.02D * 0.02D;

    private final WorldRuntimeStore<SummonBullet> summonStore = new WorldRuntimeStore<>();
    private final SummonOwnershipIndex ownershipIndex = new SummonOwnershipIndex();
    private final SummonSnapshotFactory snapshotFactory = new SummonSnapshotFactory();
    private final SummonSyncService syncService = new SummonSyncService();
    private final Map<UUID, OwnerCommandTarget> ownerCommandTargets = new HashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1000000);
    private final SummonSlotManager slotManager = new SummonSlotManager();
    private final List<SummonBullet> tickSummonsScratch = new ArrayList<>();
    private final List<Integer> deadSummonIdsScratch = new ArrayList<>();

    private SummonManager() {}

    public static SummonManager getInstance() {
        return INSTANCE;
    }

    public SummonSlotManager getSlotManager() {
        return slotManager;
    }

    public void setPlayerMaxSlots(EntityPlayer player, int slots) {
        if (player == null) {
            return;
        }
        slotManager.setMaxSlots(player, slots);
        reconcileOwnerSummons(player);
    }

    public int spawnSummon(World world, EntityLivingBase owner, SummonDefinition definition) {
        return spawnSummon(world, owner, definition, null);
    }

    public int spawnSummon(World world, EntityLivingBase owner, SummonDefinition definition, Vec3d position) {
        if (world.isRemote) {
            return -1;
        }
        if (owner == null) {
            throw new IllegalArgumentException("Owner must not be null");
        }

        if (owner instanceof EntityPlayer) {
            if (!slotManager.reserve((EntityPlayer) owner, definition.getSlotCost())) {
                return -1;
            }
        }

        int id = nextId.getAndIncrement();
        int formationIndex = getOwnedSummonCount(owner.getUniqueID());
        Vec3d spawnPos = position == null ? getDefaultSpawnPosition(owner, definition) : position;
        SummonBullet summon = new SummonBullet(id, spawnPos, new Vec3d(0, 0, 0), definition, owner, formationIndex, world.getTotalWorldTime());
        summonStore.put(world, summon);
        indexSummon(world, summon);
        MinecraftForge.EVENT_BUS.post(new SummonSpawnEvent(world, createSummonSnapshot(summon)));
        sendSpawn(world, summon);
        return id;
    }

    public void removeSummon(World world, int id) {
        removeSummon(world, id, LifecycleRemoveReason.API_REQUEST);
    }

    public void removeSummon(World world, int id, LifecycleRemoveReason reason) {
        SummonBullet summon = summonStore.remove(world, id);
        if (summon == null) {
            return;
        }
        deindexSummon(world, summon);
        cleanupSummonActors(world, summon);
        releaseSlots(summon, world);
        MinecraftForge.EVENT_BUS.post(new SummonRemoveEvent(world, createSummonSnapshot(summon), reason));
        syncService.sendRemove(world, id);
    }

    public boolean hasSummon(World world, int id) {
        return summonStore.hasLive(world, id);
    }

    public SummonSnapshot getSummonSnapshot(World world, int id) {
        return summonStore.getLiveSnapshot(world, id, snapshotFactory);
    }

    public int getSummonCount(World world) {
        return summonStore.countLive(world);
    }

    public List<Integer> getSummonIds(World world) {
        return summonStore.getLiveIds(world);
    }

    public List<SummonSnapshot> getSummonSnapshots(World world) {
        return summonStore.getLiveSnapshots(world, snapshotFactory);
    }

    public List<SummonSnapshot> getOwnedSummonSnapshots(World world, UUID ownerId) {
        if (world == null || ownerId == null) {
            return Collections.emptyList();
        }
        List<SummonBullet> owned = getOwnedSummons(ownerId);
        if (owned.isEmpty()) {
            return Collections.emptyList();
        }
        List<SummonSnapshot> snapshots = new ArrayList<>();
        for (SummonBullet summon : owned) {
            if (!summon.isDead() && getLiveSummon(world, summon.getId()) == summon) {
                snapshots.add(snapshotFactory.create(summon));
            }
        }
        return snapshots;
    }

    public void updateSummonPosition(World world, int id, Vec3d position) {
        SummonBullet summon = getLiveSummon(world, id);
        if (summon == null) {
            return;
        }
        summon.setPosition(position);
        syncSummon(world, summon, SPacketSummon.FLAG_POSITION);
    }

    public void updateSummonVelocity(World world, int id, Vec3d velocity) {
        SummonBullet summon = getLiveSummon(world, id);
        if (summon == null) {
            return;
        }
        summon.setVelocity(velocity);
        syncSummon(world, summon, SPacketSummon.FLAG_VELOCITY);
    }

    public void updateSummonMotion(World world, int id, Vec3d position, Vec3d velocity) {
        SummonBullet summon = getLiveSummon(world, id);
        if (summon == null) {
            return;
        }
        summon.setPosition(position);
        summon.setVelocity(velocity);
        syncSummon(world, summon, SPacketSummon.FLAG_POSITION | SPacketSummon.FLAG_VELOCITY);
    }

    public void updateSummonLife(World world, int id, int life) {
        SummonBullet summon = getLiveSummon(world, id);
        if (summon == null) {
            return;
        }
        summon.setLife(life);
        if (life <= 0) {
            removeSummon(world, id, LifecycleRemoveReason.API_REQUEST);
            return;
        }
        syncSummon(world, summon, SPacketSummon.FLAG_LIFE);
    }

    public void updateSummonTexture(World world, int id, String texture) {
        updateSummonVisual(world, id, texture, null, null, SPacketBulletVisual.FLAG_TEXTURE);
    }

    public void updateSummonRendererType(World world, int id, String rendererType) {
        updateSummonVisual(world, id, null, rendererType, null, SPacketBulletVisual.FLAG_RENDERER);
    }

    public void updateSummonRenderState(World world, int id, String renderState) {
        updateSummonVisual(world, id, null, null, renderState, SPacketBulletVisual.FLAG_RENDER_STATE);
    }

    public void updateSummonVisual(World world, int id, String texture, String rendererType, String renderState, int flags) {
        SummonBullet summon = getLiveSummon(world, id);
        if (summon == null || flags == 0) {
            return;
        }
        if ((flags & SPacketBulletVisual.FLAG_TEXTURE) != 0) {
            summon.setTexture(texture);
        }
        if ((flags & SPacketBulletVisual.FLAG_RENDERER) != 0) {
            summon.setRendererType(rendererType);
        }
        if ((flags & SPacketBulletVisual.FLAG_RENDER_STATE) != 0) {
            summon.setRenderState(renderState);
        }
        syncSummonVisual(world, summon, flags);
    }

    public void updateSummonTarget(World world, int id, EntityLivingBase target) {
        SummonBullet summon = getLiveSummon(world, id);
        if (summon == null) {
            return;
        }
        updateOwnerCommandTarget(summon.getOwnerId(), world, target, world.getTotalWorldTime(), world.rand);
    }

    public void updateSummonState(World world, int id, SummonState state) {
        SummonBullet summon = getLiveSummon(world, id);
        if (summon == null || state == null) {
            return;
        }
        SummonState previousState = summon.getState();
        summon.setState(state);
        if (previousState != state) {
            MinecraftForge.EVENT_BUS.post(new SummonStateChangedEvent(
                    world,
                    new SummonHandle(world, summon.getId()),
                    previousState,
                    state
                ));
        }
    }

    public void retargetSummon(World world, int id) {
        SummonBullet summon = getLiveSummon(world, id);
        if (summon == null) {
            return;
        }
        EntityLivingBase owner = summon.getOwnerEntity(world);
        if (owner == null || owner.isDead) {
            removeSummon(world, id, LifecycleRemoveReason.OWNER_LOST);
            return;
        }

        int previousTargetId = summon.getTargetEntityId();
        SummonContext context = createContext(world, summon, owner, world.getTotalWorldTime());
        if (summon.getDefinition().getTargetSelector() != null) {
            context.setAutoTarget(summon.getDefinition().getTargetSelector().selectTarget(context));
        } else {
            context.clearTarget();
        }
        summon.resetRetargetCooldown();
        if (previousTargetId != summon.getTargetEntityId()) {
            MinecraftForge.EVENT_BUS.post(new SummonTargetChangedEvent(
                    world,
                    new SummonHandle(world, summon.getId()),
                    previousTargetId,
                    summon.getTargetEntityId()
            ));
        }
    }

    public void returnSummonToOwner(World world, int id) {
        SummonBullet summon = getLiveSummon(world, id);
        if (summon == null) {
            return;
        }
        int previousTargetId = summon.getTargetEntityId();
        SummonState previousState = summon.getState();
        summon.clearTarget();
        summon.resetRetargetCooldown();
        summon.setState(SummonState.RETURNING);
        emitStateChanges(world, summon, previousState, previousTargetId);
    }

    public void updateSummonMode(World world, int id, String mode) {
        updateSummonRuntime(world, id, runtime -> {
            if (mode == null || mode.trim().isEmpty()) {
                runtime.removeTag(SummonContext.MODE_KEY);
            } else {
                runtime.setString(SummonContext.MODE_KEY, mode);
            }
        });
    }

    public void clearSummonMode(World world, int id) {
        updateSummonMode(world, id, null);
    }

    public void updateSummonIntParam(World world, int id, String key, int value) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        updateSummonRuntime(world, id, runtime -> runtime.setInteger(key, value));
    }

    public void updateSummonFloatParam(World world, int id, String key, float value) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        updateSummonRuntime(world, id, runtime -> runtime.setFloat(key, value));
    }

    public void updateSummonBoolParam(World world, int id, String key, boolean value) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        updateSummonRuntime(world, id, runtime -> runtime.setBoolean(key, value));
    }

    public void updateSummonStringParam(World world, int id, String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        updateSummonRuntime(world, id, runtime -> {
            if (value == null) {
                runtime.removeTag(key);
            } else {
                runtime.setString(key, value);
            }
        });
    }

    public void clearSummonParam(World world, int id, String key) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        updateSummonRuntime(world, id, runtime -> runtime.removeTag(key));
    }

    public List<SummonBullet> getOwnedSummons(UUID ownerId) {
        return ownershipIndex.getOwnedSummons(ownerId, summonStore);
    }

    public int getOwnedSummonCount(UUID ownerId) {
        return ownershipIndex.getOwnedSummonCount(ownerId, summonStore);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerLoggedOutEvent event) {
        ownerCommandTargets.remove(event.player.getUniqueID());
        removeOwnedSummons(event.player.getUniqueID(), LifecycleRemoveReason.PLAYER_LOGOUT);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        ownerCommandTargets.remove(event.player.getUniqueID());
        removeOwnedSummons(event.player.getUniqueID(), LifecycleRemoveReason.DIMENSION_CHANGE);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote || event.phase != TickEvent.Phase.END) {
            return;
        }
        Map<Integer, SummonBullet> summonMap = summonStore.getWorldEntries(event.world);
        if (summonMap == null || summonMap.isEmpty()) {
            return;
        }

        tickSummonsScratch.clear();
        tickSummonsScratch.addAll(summonMap.values());
        long worldTick = event.world.getTotalWorldTime();
        Set<UUID> reconciledOwners = new HashSet<>();
        for (SummonBullet summon : tickSummonsScratch) {
            if (summon.isDead()) {
                continue;
            }
            if (!summonStore.isCurrent(event.world, summon.getId(), summon)) {
                continue;
            }

            EntityLivingBase owner = summon.getOwnerEntity(event.world);
            if (owner == null || owner.isDead) {
                removeSummon(event.world, summon.getId(), LifecycleRemoveReason.OWNER_LOST);
                continue;
            }

            if (owner instanceof EntityPlayer && reconciledOwners.add(owner.getUniqueID())) {
                reconcileOwnerSummons((EntityPlayer) owner);
            }
            if (summon.isDead() || !summonStore.isCurrent(event.world, summon.getId(), summon)) {
                continue;
            }

            SummonState previousState = summon.getState();
            int previousTargetId = summon.getTargetEntityId();

            summon.tickCooldowns();
            boolean forcedRecovery = isOutsideLeash(owner, summon);
            EntityLivingBase ownerCommandTarget = resolveOwnerCommandTarget(event.world, owner.getUniqueID());
            syncOwnerCommandTarget(owner, summon, ownerCommandTarget, forcedRecovery);
            EntityLivingBase target = resolveTrackedTarget(event.world, owner, summon, forcedRecovery);

            SummonContext context = new SummonContext(this, event.world, summon, owner, summon.getDefinition(), worldTick, target);
            if (context.getTarget() == null && summon.shouldRetarget() && summon.getDefinition().getTargetSelector() != null) {
                context.setAutoTarget(summon.getDefinition().getTargetSelector().selectTarget(context));
                summon.resetRetargetCooldown();
            }
            if (summon.getDefinition().getMoveController() != null) {
                if (context.getTarget() == null) {
                    summon.getDefinition().getMoveController().tickNoTargetMovement(context);
                } else {
                    summon.getDefinition().getMoveController().tickCombatMovement(context);
                }
            }
            summon.update(event.world);
            handleSummonBodyCollision(summon, event.world, worldTick);
            if (summon.getDefinition().getAttackPattern() != null) {
                summon.getDefinition().getAttackPattern().tickAttack(context);
            }

            emitStateChanges(event.world, summon, previousState, previousTargetId);

            if (summon.shouldSync()) {
                int snapshotFlags = getSnapshotFlags(summon, worldTick);
                if (snapshotFlags != 0) {
                    sendSnapshot(event.world, summon, snapshotFlags);
                    summon.markSynced(worldTick);
                }
                summon.resetSyncCooldown();
            }
        }

        deadSummonIdsScratch.clear();
        for (SummonBullet summon : summonMap.values()) {
            if (summon.isDead()) {
                deadSummonIdsScratch.add(summon.getId());
            }
        }
        for (Integer deadId : deadSummonIdsScratch) {
            removeSummon(event.world, deadId, LifecycleRemoveReason.EXPIRED);
        }
        tickSummonsScratch.clear();
        deadSummonIdsScratch.clear();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        Map<Integer, SummonBullet> removed = summonStore.removeWorld(event.getWorld());
        if (removed == null) {
            return;
        }
        for (SummonBullet summon : removed.values()) {
            deindexSummon(event.getWorld(), summon);
            cleanupSummonActors(event.getWorld(), summon);
            releaseSlots(summon, event.getWorld());
            MinecraftForge.EVENT_BUS.post(new SummonRemoveEvent(event.getWorld(), createSummonSnapshot(summon), LifecycleRemoveReason.WORLD_UNLOAD));
        }
    }

    private Vec3d getDefaultSpawnPosition(EntityLivingBase owner, SummonDefinition definition) {
        return owner.getPositionVector().add(0, owner.getEyeHeight() * 0.7D + definition.getIdleHeight(), 0);
    }

    private void sendSpawn(World world, SummonBullet summon) {
        syncService.sendSpawn(world, summon);
    }

    private void sendSnapshot(World world, SummonBullet summon, int flags) {
        syncService.sendSnapshot(world, summon, flags);
    }

    private void emitStateChanges(World world, SummonBullet summon, SummonState previousState, int previousTargetId) {
        if (previousState != summon.getState()) {
            MinecraftForge.EVENT_BUS.post(new SummonStateChangedEvent(
                    world,
                    new SummonHandle(world, summon.getId()),
                    previousState,
                    summon.getState()
            ));
        }

        if (previousTargetId != summon.getTargetEntityId()) {
            MinecraftForge.EVENT_BUS.post(new SummonTargetChangedEvent(
                    world,
                    new SummonHandle(world, summon.getId()),
                    previousTargetId,
                    summon.getTargetEntityId()
            ));
        }
    }

    private void releaseSlots(SummonBullet summon, World world) {
        if (summon.hasReleasedSlots()) {
            return;
        }
        EntityLivingBase owner = summon.getOwnerEntity(world);
        if (owner instanceof EntityPlayer) {
            slotManager.release((EntityPlayer) owner, summon.getSlotCost());
        } else {
            slotManager.release(summon.getOwnerId(), summon.getSlotCost());
        }
        summon.markSlotsReleased();
    }

    private void cleanupSummonActors(World world, SummonBullet summon) {
        if (world == null || summon == null) {
            return;
        }
        if (summon.hasActiveLaser()) {
            DanmakuManager.getInstance().removeLaser(world, summon.getActiveLaserId());
            summon.clearActiveLaserId();
        }
    }

    public void reconcileOwnerSummons(EntityPlayer player) {
        if (player == null) {
            return;
        }
        reconcileOwnerSummons(player.getUniqueID(), slotManager.getMaxSlots(player));
    }

    private void removeOwnedSummons(UUID ownerId, LifecycleRemoveReason reason) {
        if (ownerId == null) {
            return;
        }
        List<SummonOwnershipIndex.OwnedSummonRef> removeRefs = ownershipIndex.collectOwnedRefs(ownerId, summonStore);
        for (SummonOwnershipIndex.OwnedSummonRef ref : removeRefs) {
            removeSummon(ref.world, ref.summonId, reason);
        }
    }

    private EntityLivingBase resolveTrackedTarget(World world, EntityLivingBase owner, SummonBullet summon, boolean forcedRecovery) {
        if (forcedRecovery) {
            summon.clearTarget();
            return null;
        }

        EntityLivingBase target = summon.getTarget(world);
        if (!isValidTrackedTarget(owner, summon, target)) {
            summon.clearTarget();
            return null;
        }

        if (summon.getTargetSource() == SummonTargetSource.COMMAND && !canAcceptCommandTarget(owner, summon, target)) {
            summon.clearTarget();
            return null;
        }
        return target;
    }

    private void syncOwnerCommandTarget(EntityLivingBase owner, SummonBullet summon, EntityLivingBase ownerCommandTarget, boolean forcedRecovery) {
        if (summon == null) {
            return;
        }

        if (forcedRecovery || ownerCommandTarget == null || !canAcceptCommandTarget(owner, summon, ownerCommandTarget)) {
            if (summon.getTargetSource() == SummonTargetSource.COMMAND) {
                summon.clearTarget();
            }
            return;
        }

        if (summon.getTargetSource() == SummonTargetSource.COMMAND
                && summon.getTargetEntityId() == ownerCommandTarget.getEntityId()) {
            return;
        }
        summon.setTarget(ownerCommandTarget, SummonTargetSource.COMMAND);
    }

    private boolean isValidTrackedTarget(EntityLivingBase owner, SummonBullet summon, EntityLivingBase target) {
        if (target == null || target.isDead) {
            return false;
        }
        double followRangeSq = summon.getDefinition().getFollowRange() * summon.getDefinition().getFollowRange();
        return target.getDistanceSq(owner) <= followRangeSq;
    }

    private boolean canAcceptCommandTarget(EntityLivingBase owner, SummonBullet summon, EntityLivingBase target) {
        if (owner == null || summon == null || target == null) {
            return false;
        }
        if (!allowsCommandResponse(summon)) {
            return false;
        }
        double followRangeSq = summon.getDefinition().getFollowRange() * summon.getDefinition().getFollowRange();
        return target.getDistanceSq(owner) <= followRangeSq;
    }

    private boolean allowsCommandResponse(SummonBullet summon) {
        SummonCommandResponsePolicy policy = summon.getDefinition().getCommandResponsePolicy();
        if (policy == null) {
            return true;
        }

        switch (policy) {
            case IGNORE_COMMAND:
                return false;
            case COMBAT_ONLY:
                return summon.getState() == SummonState.CHASING_TARGET
                        || summon.getState() == SummonState.ATTACKING;
            case STRICT_LOCK:
            default:
                return true;
        }
    }

    private boolean isOutsideLeash(EntityLivingBase owner, SummonBullet summon) {
        if (owner == null || summon == null) {
            return false;
        }
        double leashRange = summon.getDefinition().getLeashRange();
        return summon.getPosition().squareDistanceTo(owner.getPositionVector()) > leashRange * leashRange;
    }

    private void updateOwnerCommandTarget(UUID ownerId, World world, EntityLivingBase target, long worldTick, Random random) {
        if (ownerId == null) {
            return;
        }
        if (target == null) {
            ownerCommandTargets.remove(ownerId);
            return;
        }

        OwnerCommandTarget command = ownerCommandTargets.computeIfAbsent(ownerId, ignored -> new OwnerCommandTarget());
        if (command.commandTick != worldTick) {
            command.commandTick = worldTick;
            command.dimension = world.provider.getDimension();
            command.targetEntityId = target.getEntityId();
            command.candidateCount = 1;
            return;
        }

        command.candidateCount = Math.max(1, command.candidateCount) + 1;
        if (random != null && random.nextInt(command.candidateCount) != 0) {
            return;
        }
        command.dimension = world.provider.getDimension();
        command.targetEntityId = target.getEntityId();
    }

    private EntityLivingBase resolveOwnerCommandTarget(World world, UUID ownerId) {
        if (world == null || ownerId == null) {
            return null;
        }

        OwnerCommandTarget command = ownerCommandTargets.get(ownerId);
        if (command == null) {
            return null;
        }
        if (command.dimension != world.provider.getDimension()) {
            return null;
        }

        net.minecraft.entity.Entity entity = world.getEntityByID(command.targetEntityId);
        if (!(entity instanceof EntityLivingBase) || entity.isDead) {
            ownerCommandTargets.remove(ownerId);
            return null;
        }
        return (EntityLivingBase) entity;
    }

    private void reconcileOwnerSummons(UUID ownerId, int maxSlots) {
        if (ownerId == null) {
            return;
        }
        List<SummonOwnershipIndex.OwnedSummonRef> ownedSummons = ownershipIndex.collectOwnedRefs(ownerId, summonStore);
        if (ownedSummons.isEmpty()) {
            return;
        }

        int used = 0;
        for (SummonOwnershipIndex.OwnedSummonRef ref : ownedSummons) {
            used += Math.max(0, ref.summon.getSlotCost());
        }
        if (used <= maxSlots) {
            return;
        }

        ownedSummons.sort((a, b) -> {
            int byTick = Long.compare(b.summon.getSpawnTick(), a.summon.getSpawnTick());
            if (byTick != 0) {
                return byTick;
            }
            return Integer.compare(b.summon.getId(), a.summon.getId());
        });
        for (SummonOwnershipIndex.OwnedSummonRef ref : ownedSummons) {
            if (used <= maxSlots) {
                break;
            }
            if (ref.summon.isDead()) {
                continue;
            }
            used -= Math.max(0, ref.summon.getSlotCost());
            removeSummon(ref.world, ref.summon.getId(), LifecycleRemoveReason.SLOT_RECONCILE);
        }
    }

    private void handleSummonBodyCollision(SummonBullet summon, World world, long worldTick) {
        if (summon.isDead()) {
            return;
        }
        if (!summon.hasCollision()) {
            return;
        }

        double x = summon.getPosX();
        double y = summon.getPosY();
        double z = summon.getPosZ();
        double radius = summon.getCollisionShape().getBroadphaseRadius();
        AxisAlignedBB searchBox = radius > 0.0D
                ? new AxisAlignedBB(x, y, z, x, y, z).grow(radius + 4.0D)
                : new AxisAlignedBB(x, y, z, x, y, z).grow(4.0D);

        List<EntityLivingBase> candidates = world.getEntitiesWithinAABB(EntityLivingBase.class, searchBox);
        for (EntityLivingBase entity : candidates) {
            if (!canSummonCollide(world, summon, entity)) {
                continue;
            }
            if (!summon.getCollisionShape().checkCollision(x, y, z, entity)) {
                continue;
            }
            if (!summon.canTriggerContact(entity, worldTick)) {
                continue;
            }
            handleSummonCollision(summon, world, entity, worldTick);
            if (summon.isDead()) {
                return;
            }
        }
    }

    private void handleSummonCollision(SummonBullet summon, World world, EntityLivingBase entity, long worldTick) {
        CollisionContext ctx = new CollisionContext(summon, world, entity);
        BulletCollisionEvent eventBus = new BulletCollisionEvent(world, summon, entity, ctx);
        MinecraftForge.EVENT_BUS.post(eventBus);
        if (!eventBus.isCanceled()) {
            summon.handleHit(ctx);
            if (!ctx.canceled) {
                entity.attackEntityFrom(DamageSource.GENERIC, ctx.damage);
            }
        }
        summon.markContactTriggered(entity, worldTick);
    }

    private boolean canSummonCollide(World world, SummonBullet summon, EntityLivingBase entity) {
        if (entity == null || entity.isDead) {
            return false;
        }
        EntityLivingBase owner = summon.getShooter();
        if (entity == owner) {
            return false;
        }
        if (summon.getOwnerId().equals(entity.getUniqueID())) {
            return false;
        }
        if (entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.disableDamage) {
            return false;
        }

        boolean defaultAllowed = owner == null || !owner.isOnSameTeam(entity);
        CombatRelation relation = CombatRelationResolverRegistry.resolveSummon(world, summon, entity);
        if (relation == CombatRelation.DENY) {
            return false;
        }
        return relation == CombatRelation.ALLOW || defaultAllowed;
    }

    private SummonSnapshot createSummonSnapshot(SummonBullet summon) {
        return snapshotFactory.create(summon);
    }

    private int getSnapshotFlags(SummonBullet summon, long worldTick) {
        Vec3d lastPos = summon.getLastSyncedPosition();
        Vec3d lastVel = summon.getLastSyncedVelocity();
        Vec3d pos = summon.getPosition();
        Vec3d vel = summon.getVelocity();

        if (lastPos == null || lastVel == null) {
            return SPacketSummon.FLAG_POSITION | SPacketSummon.FLAG_VELOCITY | SPacketSummon.FLAG_LIFE;
        }

        int flags = 0;
        if (pos.squareDistanceTo(lastPos) >= SNAPSHOT_POS_EPS_SQ) {
            flags |= SPacketSummon.FLAG_POSITION;
        }
        if (vel.squareDistanceTo(lastVel) >= SNAPSHOT_VEL_EPS_SQ) {
            flags |= SPacketSummon.FLAG_VELOCITY;
        }

        int maxSilentTicks = Math.max(10, summon.getDefinition().getSyncIntervalTicks() * 5);
        if (worldTick - summon.getLastSyncWorldTick() >= maxSilentTicks) {
            flags |= SPacketSummon.FLAG_LIFE;
        }
        return flags;
    }

    private SummonBullet getLiveSummon(World world, int id) {
        return summonStore.getLive(world, id);
    }

    private SummonContext createContext(World world, SummonBullet summon, EntityLivingBase owner, long worldTick) {
        EntityLivingBase target = summon.getTarget(world);
        if (target != null) {
            double followRange = summon.getDefinition().getFollowRange();
            if (target.isDead || target.getDistanceSq(owner) > followRange * followRange) {
                target = null;
            }
        }
        return new SummonContext(this, world, summon, owner, summon.getDefinition(), worldTick, target);
    }

    private void syncSummon(World world, SummonBullet summon, int flags) {
        if (summon == null) {
            return;
        }
        if (summon.isDead() || summon.getLife() <= 0) {
            removeSummon(world, summon.getId(), LifecycleRemoveReason.API_REQUEST);
            return;
        }
        syncService.sendSnapshot(world, summon, flags);
        summon.markSynced(world.getTotalWorldTime());
        summon.resetSyncCooldown();
    }

    private void syncSummonVisual(World world, SummonBullet summon, int flags) {
        if (summon == null) {
            return;
        }
        syncService.sendVisual(world, summon, flags);
    }

    private void updateSummonRuntime(World world, int id, Consumer<NBTTagCompound> mutation) {
        SummonBullet summon = getLiveSummon(world, id);
        if (summon == null || mutation == null) {
            return;
        }

        NBTTagCompound root = summon.getCustomData();
        if (root == null) {
            root = new NBTTagCompound();
        }

        NBTTagCompound runtime = root.hasKey(SummonContext.RUNTIME_ROOT_KEY)
                ? root.getCompoundTag(SummonContext.RUNTIME_ROOT_KEY)
                : new NBTTagCompound();
        mutation.accept(runtime);
        if (runtime.isEmpty()) {
            root.removeTag(SummonContext.RUNTIME_ROOT_KEY);
        } else {
            root.setTag(SummonContext.RUNTIME_ROOT_KEY, runtime);
        }
        summon.setCustomData(root);
    }

    private void indexSummon(World world, SummonBullet summon) {
        ownershipIndex.index(world, summon);
    }

    private void deindexSummon(World world, SummonBullet summon) {
        ownershipIndex.deindex(world, summon);
    }

    private static final class OwnerCommandTarget {
        private int targetEntityId = -1;
        private int dimension;
        private long commandTick = Long.MIN_VALUE;
        private int candidateCount;
    }
}
