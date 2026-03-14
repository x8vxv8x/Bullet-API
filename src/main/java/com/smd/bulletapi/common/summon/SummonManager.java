package com.smd.bulletapi.common.summon;

import com.smd.bulletapi.api.LaserApi;
import com.smd.bulletapi.api.annotation.InternalApi;
import com.smd.bulletapi.api.handle.SummonHandle;
import com.smd.bulletapi.api.snapshot.SummonSnapshot;
import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.event.BulletCollisionEvent;
import com.smd.bulletapi.event.lifecycle.LifecycleRemoveReason;
import com.smd.bulletapi.event.lifecycle.SummonRemoveEvent;
import com.smd.bulletapi.event.lifecycle.SummonSpawnEvent;
import com.smd.bulletapi.event.lifecycle.SummonStateChangedEvent;
import com.smd.bulletapi.event.lifecycle.SummonTargetChangedEvent;
import com.smd.bulletapi.network.PacketHandler;
import com.smd.bulletapi.network.SPacketSummon;
import com.smd.bulletapi.server.summon.SummonBullet;
import com.smd.bulletapi.spi.combat.CombatRelation;
import com.smd.bulletapi.spi.combat.CombatRelationResolverRegistry;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@InternalApi
public class SummonManager {
    private static final SummonManager INSTANCE = new SummonManager();

    private final Map<World, Map<Integer, SummonBullet>> worldSummons = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1000000);
    private final SummonSlotManager slotManager = new SummonSlotManager();

    private SummonManager() {}

    public static SummonManager getInstance() {
        return INSTANCE;
    }

    public SummonSlotManager getSlotManager() {
        return slotManager;
    }

    public void setPlayerMaxSlots(EntityPlayer player, int slots) {
        if (player == null) return;
        slotManager.setMaxSlots(player, slots);
        reconcileOwnerSummons(player);
    }

    public int spawnSummon(World world, EntityLivingBase owner, SummonDefinition definition) {
        if (world.isRemote) return -1;
        if (owner == null) throw new IllegalArgumentException("Owner must not be null");

        if (owner instanceof EntityPlayer) {
            if (!slotManager.reserve((EntityPlayer) owner, definition.getSlotCost())) {
                return -1;
            }
        }

        int id = nextId.getAndIncrement();
        int formationIndex = getOwnedSummons(owner.getUniqueID()).size();
        Vec3d spawnPos = owner.getPositionVector().add(0, owner.getEyeHeight() * 0.7D + definition.getIdleHeight(), 0);
        SummonBullet summon = new SummonBullet(id, spawnPos, new Vec3d(0, 0, 0), definition, owner, formationIndex, world.getTotalWorldTime());
        getWorldMap(world).put(id, summon);
        MinecraftForge.EVENT_BUS.post(new SummonSpawnEvent(world, createSummonSnapshot(summon)));
        sendSpawn(world, summon);
        return id;
    }

    public void removeSummon(World world, int id) {
        removeSummon(world, id, LifecycleRemoveReason.API_REQUEST);
    }

    public void removeSummon(World world, int id, LifecycleRemoveReason reason) {
        Map<Integer, SummonBullet> map = worldSummons.get(world);
        if (map == null) return;
        SummonBullet summon = map.remove(id);
        if (summon == null) return;
        cleanupSummonActors(world, summon);
        releaseSlots(summon, world);
        MinecraftForge.EVENT_BUS.post(new SummonRemoveEvent(world, createSummonSnapshot(summon), reason));
        PacketHandler.sendToDimension(SPacketSummon.createRemove(id), world.provider.getDimension());
    }

    public boolean hasSummon(World world, int id) {
        Map<Integer, SummonBullet> map = worldSummons.get(world);
        if (map == null) return false;
        SummonBullet summon = map.get(id);
        return summon != null && !summon.isDead();
    }

    public SummonSnapshot getSummonSnapshot(World world, int id) {
        Map<Integer, SummonBullet> map = worldSummons.get(world);
        if (map == null) return null;
        SummonBullet summon = map.get(id);
        return summon == null ? null : createSummonSnapshot(summon);
    }

    public int getSummonCount(World world) {
        Map<Integer, SummonBullet> map = worldSummons.get(world);
        return map == null ? 0 : map.size();
    }

    public List<SummonBullet> getOwnedSummons(UUID ownerId) {
        if (ownerId == null) return Collections.emptyList();
        List<SummonBullet> summons = new ArrayList<>();
        for (Map<Integer, SummonBullet> worldMap : worldSummons.values()) {
            for (SummonBullet summon : worldMap.values()) {
                if (ownerId.equals(summon.getOwnerId()) && !summon.isDead()) {
                    summons.add(summon);
                }
            }
        }
        return summons;
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerLoggedOutEvent event) {
        removeOwnedSummons(event.player.getUniqueID(), LifecycleRemoveReason.PLAYER_LOGOUT);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        removeOwnedSummons(event.player.getUniqueID(), LifecycleRemoveReason.DIMENSION_CHANGE);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote || event.phase != TickEvent.Phase.END) return;
        Map<Integer, SummonBullet> summonMap = worldSummons.get(event.world);
        if (summonMap == null || summonMap.isEmpty()) return;

        List<SummonBullet> summons = new ArrayList<>(summonMap.values());
        long worldTick = event.world.getTotalWorldTime();
        Set<UUID> reconciledOwners = new HashSet<>();
        for (SummonBullet summon : summons) {
            if (summon.isDead()) continue;
            if (summonMap.get(summon.getId()) != summon) continue;

            EntityLivingBase owner = summon.getOwnerEntity(event.world);
            if (owner == null || owner.isDead) {
                removeSummon(event.world, summon.getId(), LifecycleRemoveReason.OWNER_LOST);
                continue;
            }

            if (owner instanceof EntityPlayer && reconciledOwners.add(owner.getUniqueID())) {
                reconcileOwnerSummons((EntityPlayer) owner);
            }
            if (summon.isDead() || summonMap.get(summon.getId()) != summon) {
                continue;
            }

            SummonState previousState = summon.getState();
            int previousTargetId = summon.getTargetEntityId();

            summon.tickCooldowns();
            EntityLivingBase target = summon.getTarget(event.world);
            if (target == null || target.isDead || target.getDistanceSq(owner) > summon.getDefinition().getFollowRange() * summon.getDefinition().getFollowRange()) {
                target = null;
            }

            SummonContext context = new SummonContext(this, event.world, summon, owner, summon.getDefinition(), worldTick, target);
            if (summon.shouldRetarget() && summon.getDefinition().getTargetSelector() != null) {
                context.setTarget(summon.getDefinition().getTargetSelector().selectTarget(context));
                summon.resetRetargetCooldown();
            }
            if (summon.getDefinition().getMoveController() != null) {
                summon.getDefinition().getMoveController().tickMovement(context);
            }
            summon.update(event.world);
            handleSummonBodyCollision(summon, event.world, worldTick);
            if (summon.getDefinition().getAttackPattern() != null) {
                summon.getDefinition().getAttackPattern().tickAttack(context);
            }

            emitStateChanges(event.world, summon, previousState, previousTargetId);

            if (summon.shouldSync()) {
                sendSnapshot(event.world, summon);
                summon.resetSyncCooldown();
            }
        }

        List<Integer> deadIds = new ArrayList<>();
        for (SummonBullet summon : summonMap.values()) {
            if (summon.isDead()) {
                deadIds.add(summon.getId());
            }
        }
        for (Integer deadId : deadIds) {
            removeSummon(event.world, deadId, LifecycleRemoveReason.EXPIRED);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        Map<Integer, SummonBullet> removed = worldSummons.remove(event.getWorld());
        if (removed == null) return;
        for (SummonBullet summon : removed.values()) {
            cleanupSummonActors(event.getWorld(), summon);
            releaseSlots(summon, event.getWorld());
            MinecraftForge.EVENT_BUS.post(new SummonRemoveEvent(event.getWorld(), createSummonSnapshot(summon), LifecycleRemoveReason.WORLD_UNLOAD));
        }
    }

    private Map<Integer, SummonBullet> getWorldMap(World world) {
        return worldSummons.computeIfAbsent(world, ignored -> new ConcurrentHashMap<>());
    }

    private void sendSpawn(World world, SummonBullet summon) {
        PacketHandler.sendToDimension(SPacketSummon.createSpawn(
                summon.getId(),
                summon.getPosition(),
                summon.getVelocity(),
                summon.getLife(),
                summon.getDamage(),
                summon.getTexture(),
                summon.getColor(),
                summon.getSize(),
                summon.getRendererType(),
                summon.getCustomData()
        ), world.provider.getDimension());
    }

    private void sendSnapshot(World world, SummonBullet summon) {
        PacketHandler.sendToDimension(SPacketSummon.createSnapshot(
                summon.getId(),
                summon.getPosition(),
                summon.getVelocity(),
                summon.getLife()
        ), world.provider.getDimension());
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
        if (summon.hasReleasedSlots()) return;
        EntityLivingBase owner = summon.getOwnerEntity(world);
        if (owner instanceof EntityPlayer) {
            slotManager.release((EntityPlayer) owner, summon.getSlotCost());
        } else {
            slotManager.release(summon.getOwnerId(), summon.getSlotCost());
        }
        summon.markSlotsReleased();
    }

    private void cleanupSummonActors(World world, SummonBullet summon) {
        if (world == null || summon == null) return;
        if (summon.hasActiveLaser()) {
            LaserApi.remove(world, summon.getActiveLaserId());
            summon.clearActiveLaserId();
        }
    }

    public void reconcileOwnerSummons(EntityPlayer player) {
        if (player == null) return;
        reconcileOwnerSummons(player.getUniqueID(), slotManager.getMaxSlots(player));
    }

    private void removeOwnedSummons(UUID ownerId, LifecycleRemoveReason reason) {
        if (ownerId == null) return;
        for (Map.Entry<World, Map<Integer, SummonBullet>> entry : worldSummons.entrySet()) {
            List<Integer> removeIds = new ArrayList<>();
            for (SummonBullet summon : entry.getValue().values()) {
                if (ownerId.equals(summon.getOwnerId())) {
                    removeIds.add(summon.getId());
                }
            }
            for (Integer id : removeIds) {
                removeSummon(entry.getKey(), id, reason);
            }
        }
    }

    private void reconcileOwnerSummons(UUID ownerId, int maxSlots) {
        if (ownerId == null) return;
        List<OwnedSummonRef> ownedSummons = collectOwnedSummons(ownerId);
        if (ownedSummons.isEmpty()) return;

        int used = 0;
        for (OwnedSummonRef ref : ownedSummons) {
            used += Math.max(0, ref.summon.getSlotCost());
        }
        if (used <= maxSlots) return;

        ownedSummons.sort((a, b) -> {
            int byTick = Long.compare(b.summon.getSpawnTick(), a.summon.getSpawnTick());
            if (byTick != 0) return byTick;
            return Integer.compare(b.summon.getId(), a.summon.getId());
        });
        for (OwnedSummonRef ref : ownedSummons) {
            if (used <= maxSlots) break;
            if (ref.summon.isDead()) continue;
            used -= Math.max(0, ref.summon.getSlotCost());
            removeSummon(ref.world, ref.summon.getId(), LifecycleRemoveReason.SLOT_RECONCILE);
        }
    }

    private List<OwnedSummonRef> collectOwnedSummons(UUID ownerId) {
        List<OwnedSummonRef> owned = new ArrayList<>();
        for (Map.Entry<World, Map<Integer, SummonBullet>> entry : worldSummons.entrySet()) {
            for (SummonBullet summon : entry.getValue().values()) {
                if (ownerId.equals(summon.getOwnerId()) && !summon.isDead()) {
                    owned.add(new OwnedSummonRef(entry.getKey(), summon));
                }
            }
        }
        return owned;
    }

    private void handleSummonBodyCollision(SummonBullet summon, World world, long worldTick) {
        if (summon.isDead()) return;
        if (!summon.hasCollision()) return;

        double x = summon.getPosX();
        double y = summon.getPosY();
        double z = summon.getPosZ();
        double radius = summon.getCollisionShape().getBroadphaseRadius();
        AxisAlignedBB searchBox = radius > 0.0D
                ? new AxisAlignedBB(x, y, z, x, y, z).grow(radius + 4.0D)
                : new AxisAlignedBB(x, y, z, x, y, z).grow(4.0D);

        List<EntityLivingBase> candidates = world.getEntitiesWithinAABB(EntityLivingBase.class, searchBox);
        for (EntityLivingBase entity : candidates) {
            if (!canSummonCollide(world, summon, entity)) continue;
            if (!summon.getCollisionShape().checkCollision(x, y, z, entity)) continue;
            if (!summon.canTriggerContact(entity, worldTick)) continue;
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
        if (entity == null || entity.isDead) return false;
        EntityLivingBase owner = summon.getShooter();
        if (entity == owner) return false;
        if (summon.getOwnerId().equals(entity.getUniqueID())) return false;
        if (entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.disableDamage) return false;

        boolean defaultAllowed = owner == null || !owner.isOnSameTeam(entity);
        CombatRelation relation = CombatRelationResolverRegistry.resolveSummon(world, summon, entity);
        if (relation == CombatRelation.DENY) {
            return false;
        }
        return relation == CombatRelation.ALLOW || defaultAllowed;
    }

    private SummonSnapshot createSummonSnapshot(SummonBullet summon) {
        return new SummonSnapshot(
                summon.getId(),
                summon.getDefinitionId(),
                summon.getOwnerId(),
                summon.getPosition(),
                summon.getVelocity(),
                summon.getLife(),
                summon.getDamage(),
                summon.getColor(),
                summon.getSize(),
                summon.getRendererType(),
                summon.getCustomData(),
                summon.getState(),
                summon.getTargetEntityId(),
                summon.getSlotCost(),
                summon.getFormationIndex()
        );
    }

    private static class OwnedSummonRef {
        private final World world;
        private final SummonBullet summon;

        private OwnedSummonRef(World world, SummonBullet summon) {
            this.world = world;
            this.summon = summon;
        }
    }
}
