package com.smd.bulletapi.debug;

import com.smd.bulletapi.api.Battlefield;
import com.smd.bulletapi.api.BulletApi;
import com.smd.bulletapi.api.SummonApi;
import com.smd.bulletapi.api.handle.BulletHandle;
import com.smd.bulletapi.api.handle.SummonHandle;
import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.summon.SummonPresetKeys;
import com.smd.bulletapi.event.BulletCollisionEvent;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber
public class TestEvent {
    private static final String TEST_TEXTURE = "bulletapi:textures/entity/bullet.png";
    private static final String TEST_RENDER_STATE = "rage";
    private static final List<VisualSyncTest> VISUAL_SYNC_TESTS = new ArrayList<>();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().world.isRemote) {
            return;
        }
        if (!"arrow".equals(event.getSource().getDamageType())) {
            return;
        }
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();

       if (player.isSneaking() && player.isInWater()) {
            spawnFairyOrbTest(player);
        }
       if(player.isInWater()){
           spawnLaserEyeTest(player);
       }
    }

    @SubscribeEvent
    public static void onSummonBodyCollision(BulletCollisionEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }
        if (!(event.getHitEntity() instanceof EntityLivingBase)) {
            return;
        }

        CollisionContext ctx = event.getContext();
        if (!ctx.isSummonBody()) {
            return;
        }
        if (ctx.attackSource == null) {
            return;
        }
        EntityLivingBase hit = (EntityLivingBase) event.getHitEntity();
        ctx.damage = Math.max(ctx.damage, event.getBullet().getDamage()) * 1.25f;
        event.getWorld().playSound(null, hit.posX, hit.posY, hit.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.45F, 1.35F);
        event.getWorld().spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                hit.posX, hit.posY + hit.height * 0.5D, hit.posZ,
                0.0D, 0.02D, 0.0D);
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote || event.phase != TickEvent.Phase.END || VISUAL_SYNC_TESTS.isEmpty()) {
            return;
        }

        long worldTick = event.world.getTotalWorldTime();
        Iterator<VisualSyncTest> iterator = VISUAL_SYNC_TESTS.iterator();
        while (iterator.hasNext()) {
            VisualSyncTest test = iterator.next();
            if (test.world != event.world) {
                continue;
            }
            if (!test.isAlive()) {
                iterator.remove();
                continue;
            }
            if (worldTick < test.nextToggleTick) {
                continue;
            }
            test.toggle();
            test.nextToggleTick = worldTick + 30L;
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (VISUAL_SYNC_TESTS.isEmpty()) {
            return;
        }
        VISUAL_SYNC_TESTS.removeIf(test -> test.world == event.getWorld());
    }

    private static void spawnFairyOrbTest(EntityPlayer player) {
        if (player.world.isRemote) {
            return;
        }
        if (SummonApi.getPlayerMaxSlots(player) < 3) {
            SummonApi.setPlayerMaxSlots(player, 3);
        }

        int id = SummonApi.spawn(player.world, player, SummonPresetKeys.FAIRY_ORB);

        if (id < 0) {
            player.sendMessage(new TextComponentString(
                    String.format("§c[BulletAPI]§r 召唤失败，槽位不足：%d/%d",
                            SummonApi.getPlayerUsedSlots(player),
                            SummonApi.getPlayerMaxSlots(player))
            ));
            return;
        }

        player.sendMessage(new TextComponentString(
                String.format("§a[BulletAPI]§r fairy_orb 已召唤，id=%d，槽位 %d/%d，当前召唤物总数: §e%d",
                        id,
                        SummonApi.getPlayerUsedSlots(player),
                        SummonApi.getPlayerMaxSlots(player),
                        Battlefield.of(player.world).summons().count())
        ));
    }

    private static void spawnLaserEyeTest(EntityPlayer player) {
        if (player.world.isRemote) {
            return;
        }
        if (SummonApi.getPlayerMaxSlots(player) < 8) {
            SummonApi.setPlayerMaxSlots(player, 8);
        }

        int id = SummonApi.spawn(player.world, player, SummonPresetKeys.LASER_EYE);

        if (id < 0) {
            player.sendMessage(new TextComponentString(
                    String.format("§c[BulletAPI]§r laser_eye 召唤失败，槽位不足：%d/%d",
                            SummonApi.getPlayerUsedSlots(player),
                            SummonApi.getPlayerMaxSlots(player))
            ));
            return;
        }

        player.sendMessage(new TextComponentString(
                String.format("§a[BulletAPI]§r laser_eye 已召唤，id=%d，占用 2 槽，当前槽位 %d/%d，召唤物总数: §e%d",
                        id,
                        SummonApi.getPlayerUsedSlots(player),
                        SummonApi.getPlayerMaxSlots(player),
                        Battlefield.of(player.world).summons().count())
        ));
    }

    private static final class VisualSyncTest {
        private final net.minecraft.world.World world;
        private final int id;
        private final boolean summon;
        private long nextToggleTick;
        private boolean altState;

        private VisualSyncTest(net.minecraft.world.World world, int id, boolean summon) {
            this.world = world;
            this.id = id;
            this.summon = summon;
            this.nextToggleTick = world.getTotalWorldTime() + 30L;
        }

        private static VisualSyncTest forBullet(net.minecraft.world.World world, int id) {
            return new VisualSyncTest(world, id, false);
        }

        private static VisualSyncTest forSummon(net.minecraft.world.World world, int id) {
            return new VisualSyncTest(world, id, true);
        }

        private boolean isAlive() {
            return summon ? SummonApi.handle(world, id).exists() : BulletApi.handle(world, id).exists();
        }

        private void toggle() {
            altState = !altState;
            if (summon) {
                SummonHandle handle = SummonApi.handle(world, id);
                if (altState) {
                    handle.setVisual(null, "model_json", TEST_RENDER_STATE);
                } else {
                    handle.setVisual(TEST_TEXTURE, "billboard", null);
                }
                return;
            }

            BulletHandle handle = BulletApi.handle(world, id);
            if (altState) {
                handle.setRenderState(TEST_RENDER_STATE);
            } else {
                handle.clearRenderState();
            }
        }
    }
}
