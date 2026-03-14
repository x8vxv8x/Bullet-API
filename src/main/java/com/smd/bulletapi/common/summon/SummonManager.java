package com.smd.bulletapi.common.summon;

import com.smd.bulletapi.network.PacketHandler;
import com.smd.bulletapi.network.SPacketSummon;
import com.smd.bulletapi.server.summon.SummonBullet;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
        SummonBullet summon = new SummonBullet(id, spawnPos, new Vec3d(0, 0, 0), definition, owner, formationIndex);
        getWorldMap(world).put(id, summon);
        sendSpawn(world, summon);
        return id;
    }

    public void removeSummon(World world, int id) {
        Map<Integer, SummonBullet> map = worldSummons.get(world);
        if (map == null) return;
        SummonBullet summon = map.remove(id);
        if (summon == null) return;
        releaseSlots(summon, world);
        PacketHandler.sendToDimension(SPacketSummon.createRemove(id), world.provider.getDimension());
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
        removeOwnedSummons(event.player.getUniqueID());
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        removeOwnedSummons(event.player.getUniqueID());
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote || event.phase != TickEvent.Phase.END) return;
        Map<Integer, SummonBullet> summonMap = worldSummons.get(event.world);
        if (summonMap == null || summonMap.isEmpty()) return;

        List<SummonBullet> summons = new ArrayList<>(summonMap.values());
        long worldTick = event.world.getTotalWorldTime();
        for (SummonBullet summon : summons) {
            if (summon.isDead()) continue;
            if (summonMap.get(summon.getId()) != summon) continue;

            EntityLivingBase owner = summon.getOwnerEntity(event.world);
            if (owner == null || owner.isDead) {
                summon.markDead();
                continue;
            }

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
            if (summon.getDefinition().getAttackPattern() != null) {
                summon.getDefinition().getAttackPattern().tickAttack(context);
            }

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
            removeSummon(event.world, deadId);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        Map<Integer, SummonBullet> removed = worldSummons.remove(event.getWorld());
        if (removed == null) return;
        for (SummonBullet summon : removed.values()) {
            releaseSlots(summon, event.getWorld());
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

    private void removeOwnedSummons(UUID ownerId) {
        if (ownerId == null) return;
        for (Map.Entry<World, Map<Integer, SummonBullet>> entry : worldSummons.entrySet()) {
            List<Integer> removeIds = new ArrayList<>();
            for (SummonBullet summon : entry.getValue().values()) {
                if (ownerId.equals(summon.getOwnerId())) {
                    removeIds.add(summon.getId());
                }
            }
            for (Integer id : removeIds) {
                removeSummon(entry.getKey(), id);
            }
        }
    }
}
