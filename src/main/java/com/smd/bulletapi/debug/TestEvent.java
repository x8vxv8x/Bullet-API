package com.smd.bulletapi.debug;

import com.smd.bulletapi.api.Battlefield;
import com.smd.bulletapi.api.BulletApi;
import com.smd.bulletapi.api.LaserApi;
import com.smd.bulletapi.api.SummonApi;
import com.smd.bulletapi.api.builder.BulletBuilder;
import com.smd.bulletapi.api.handle.BulletHandle;
import com.smd.bulletapi.api.handle.SummonHandle;
import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.RenderStateData;
import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.common.collision.SphereShape;
import com.smd.bulletapi.common.summon.SummonDefinition;
import com.smd.bulletapi.common.summon.SummonPresetKeys;
import com.smd.bulletapi.common.summon.behavior.impl.RamStrikeMoveController;
import com.smd.bulletapi.event.BulletCollisionEvent;
import com.smd.bulletapi.spi.bullet.IBulletHitBehavior;
import com.smd.bulletapi.spi.laser.ILaserHitBehavior;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//@Mod.EventBusSubscriber
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
        if (!SummonPresetKeys.RAM_WISP.equals(ctx.attackSource.getSummonDefinitionId())) {
            return;
        }

        EntityLivingBase hit = (EntityLivingBase) event.getHitEntity();
        int summonId = ctx.attackSource.getSummonId();
        if (summonId >= 0) {
            SummonApi.handle(event.getWorld(), summonId)
                    .setMode(RamStrikeMoveController.MODE_BODY_HIT)
                    .setInt(RamStrikeMoveController.KEY_HIT_ENTITY_ID, hit.getEntityId());
        }

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

    private static void spawnDefaultBillboardTest(EntityPlayer player, EntityLivingBase victim) {
        double ringRadius = 8.0;
        double heightOffset = player.getEyeHeight() + 2.0;
        String texturePath = "bulletapi:textures/entity/bullet.png";
        int count = 100;
        int lifeTime = 200;          // 15秒
        float damage = 2.0F;
        ICollisionShape collisionShape = new SphereShape(8.0);

        IBulletHitBehavior hitBehavior = ctx -> {
            ctx.damage = ctx.bullet.getDamage();
            EntityLivingBase shooter = ctx.shooter;
            if (shooter instanceof EntityPlayer) {
                EntityPlayer shooterPlayer = (EntityPlayer) shooter;
                ItemStack held = ctx.shooterHeldItem;
                String itemName = held == null ? "空手" : held.getDisplayName();
                shooterPlayer.sendMessage(new TextComponentString(
                        String.format("弹幕击中 %s，玩家：%s，手持：%s",
                                ctx.hitEntity.getName(), shooterPlayer.getName(), itemName)
                ));
            }
            ctx.world.playSound(null, ctx.hitEntity.posX, ctx.hitEntity.posY, ctx.hitEntity.posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8F, 1.2F);
            ctx.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                    ctx.hitEntity.posX, ctx.hitEntity.posY + 0.5, ctx.hitEntity.posZ,
                    0, 0, 0);
            BulletApi.handle(ctx.world, ctx.bullet.getId()).remove();
        };

        Vec3d fixedCenter = player.getPositionVector().add(0, heightOffset, 0);
        Vec3d targetPos = victim.getPositionVector();

        for (int i = 0; i < count; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double dx = ringRadius * Math.cos(angle);
            double dz = ringRadius * Math.sin(angle);
            Vec3d spawnPos = fixedCenter.add(dx, 0, dz);

            // 计算从生成位置指向目标的方向向量
            Vec3d toTarget = targetPos.subtract(spawnPos);
            double speed = 0.8; // 速度大小，单位 block/tick
            Vec3d initialVel = toTarget.normalize().scale(speed);

            BulletApi.builder(player.world)
                    .position(spawnPos)
                    .velocity(initialVel)
                    .life(lifeTime)
                    .damage(damage)
                    .texture(texturePath)
                    .color(0xFF3333)
                    .size(2.0f)
                    .rendererType("billboard")
                    .collisionShape(collisionShape)
                    .hitBehavior(hitBehavior)
                    .shooter(player)
                    .shooterHeldItem(player.getHeldItemMainhand())
                    .onlyPlayer(false)
                    .spawn();
        }

        int totalBullets = Battlefield.of(player.world).bullets().count();
        player.sendMessage(new TextComponentString(
                String.format("§a[BulletAPI]§r 已生成 %d 枚直线弹幕，当前世界弹幕总数: §e%d", count, totalBullets)
        ));
    }

    private static void spawnModelTestBullets(EntityPlayer player, EntityLivingBase victim) {
        double ringRadius = 6.0;
        double heightOffset = player.getEyeHeight() + 1.0;
        int count = 120;
        int lifeTime = 120;
        float damage = 2.0F;
        double speed = 0.55;
        ICollisionShape collisionShape = new SphereShape(1.2);

        IBulletHitBehavior hitBehavior = ctx -> {
            ctx.damage = ctx.bullet.getDamage();
            BulletApi.handle(ctx.world, ctx.bullet.getId()).remove();
        };

        Vec3d fixedCenter = player.getPositionVector().add(0, heightOffset, 0);
        Vec3d targetPos = victim.getPositionVector().add(0, victim.height * 0.5, 0);

        for (int i = 0; i < count; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double dx = ringRadius * Math.cos(angle);
            double dz = ringRadius * Math.sin(angle);
            Vec3d spawnPos = fixedCenter.add(dx, 0, dz);
            Vec3d velocity = targetPos.subtract(spawnPos).normalize().scale(speed);

            BulletBuilder builder = BulletApi.builder(player.world)
                    .position(spawnPos)
                    .velocity(velocity)
                    .life(lifeTime)
                    .damage(damage)
                    .size(0.55f)
                    .collisionShape(collisionShape)
                    .hitBehavior(hitBehavior)
                    .shooter(player)
                    .shooterHeldItem(player.getHeldItemMainhand())
                    .onlyPlayer(false);

            builder.rendererType("model_json")
                    .set("model", "minecraft:furnace")
                    .set("variant", "inventory")
                    .set("scale", 0.45f)
                    .set("rot_mode", "velocity")
                    .set("tint", 0x66CCFF);

            builder.spawn();
        }

        int totalBullets = Battlefield.of(player.world).bullets().count();
        player.sendMessage(new TextComponentString(
                String.format("§a[BulletAPI]§r 模型测试已触发（潜行模式），生成 %d 枚模型弹幕，当前总数: §e%d", count, totalBullets)
        ));
    }

    private static void spawnLaserTest(EntityPlayer player) {
        ILaserHitBehavior hitBehavior = ctx -> {
            ctx.damage = ctx.laser.getDamage();
            ctx.world.playSound(null, ctx.hitEntity.posX, ctx.hitEntity.posY, ctx.hitEntity.posZ,
                    SoundEvents.BLOCK_NOTE_HARP, SoundCategory.PLAYERS, 0.4F, 1.8F);
        };

        int id = LaserApi.builder(player.world)
                .shooter(player)
                .shooterHeldItem(player.getHeldItemMainhand())
                .followShooter(true)
                .startOffset(new Vec3d(0, 0, 0))
                .startOffsetLocal(new Vec3d(0, -0.25, 0.6))
                .maxLength(28.0)
                .thickness(0.4f)
                .damage(2.5f)
                .color(0x66CCFF)
                .penetrate(true)
                .blockStops(true)
                .rendererType("laser_blast")
                .eventIntervalTicks(5)
                .hitBehavior(hitBehavior)
                .life(60)
                .spawn();

        player.sendMessage(new TextComponentString(
                String.format("§a[BulletAPI]§r 激光测试已触发，id=%d", id)
        ));
    }

    private static void spawnLaserBlastTest(EntityPlayer player) {
        ILaserHitBehavior hitBehavior = ctx -> {
            ctx.damage = ctx.laser.getDamage();
            ctx.world.playSound(null, ctx.hitEntity.posX, ctx.hitEntity.posY, ctx.hitEntity.posZ,
                    SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 0.5F, 0.9F);
        };

        int id = LaserApi.builder(player.world)
                .shooter(player)
                .shooterHeldItem(player.getHeldItemMainhand())
                .followShooter(true)
                .startOffset(new Vec3d(0, -0.25, 0))
                .maxLength(30.0)
                .thickness(0.7f)
                .damage(3.0f)
                .color(0x7FD7FF)
                .penetrate(true)
                .blockStops(true)
                .rendererType("laser_blast")
                .set("alpha", 0.85f)
                .set("segment_len", 1.1f)
                .set("core_scale", 0.55f)
                .set("shell_scale", 1.15f)
                .set("pulse_amp", 0.25f)
                .set("pulse_speed", 0.45f)
                .set("core_color", 0xFFFFFF)
                .set("shell_color", 0x7FD7FF)
                .set("shell_color_end", 0x5533FF)
                .hitBehavior(hitBehavior)
                .life(60)
                .spawn();

        player.sendMessage(new TextComponentString(
                String.format("§a[BulletAPI]§r 激光爆裂测试已触发，id=%d", id)
        ));
    }

    private static void spawnLaserPolyTest(EntityPlayer player) {
        ILaserHitBehavior hitBehavior = ctx -> ctx.damage = ctx.laser.getDamage();

        int id = LaserApi.builder(player.world)
                .shooter(player)
                .shooterHeldItem(player.getHeldItemMainhand())
                .followShooter(true)
                .startOffset(new Vec3d(0, -0.6, 0))
                .maxLength(32.0)
                .thickness(0.8f)
                .damage(3.0f)
                .color(0x66CCFF)
                .penetrate(true)
                .blockStops(true)
                .rendererType("laser_poly")
                .set("alpha", 0.8f)
                .set("poly_sides", 8)
                .set("core_scale", 0.45f)
                .set("shell_scale", 1.05f)
                .set("pulse_amp", 0.18f)
                .set("pulse_speed", 0.4f)
                .set("core_color", 0xFFFFFF)
                .set("shell_color", 0x66CCFF)
                .set("shell_color_end", 0x3366FF)
                .set("block_len", 3.2f)
                .set("block_speed", 0.4f)
                .set("block_soft", true)
                .set("block_apply_core", false)
                .set("block_color_a", 0x66CCFF)
                .set("block_color_b", 0xFF66CC)
                .set("twist_speed", 0.8f)
                .set("twist_step", 0.4f)
                .set("jitter_amp", 0.04f)
                .set("jitter_freq", 0.9f)
                .set("deco_on", true)
                .set("deco_scale", 3.35f)
                .set("deco_alpha", 0.45f)
                .set("deco_step", 1.6f)
                .set("deco_scroll", 0.1f)
                .set("deco_rot_speed", 0.2f)
                .set("deco_color", 0x88CCFF)
                .hitBehavior(hitBehavior)
                .life(60)
                .spawn();

        player.sendMessage(new TextComponentString(
                String.format("§a[BulletAPI]§r 多面体激光测试已触发，id=%d", id)
        ));
    }

    private static void spawnFairyOrbTest(EntityPlayer player) {
        if (player.world.isRemote) {
            return;
        }
        if (SummonApi.getPlayerMaxSlots(player) < 3) {
            SummonApi.setPlayerMaxSlots(player, 3);
        }

        int id = SummonApi.builder(player.world)
                .owner(player)
                .definition(SummonPresetKeys.FAIRY_ORB)
                .spawn();

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

        int id = SummonApi.builder(player.world)
                .owner(player)
                .definition(SummonPresetKeys.LASER_EYE)
                .spawn();

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

    private static void spawnRamWispTest(EntityPlayer player) {
        if (player.world.isRemote) {
            return;
        }
        if (SummonApi.getPlayerMaxSlots(player) < 8) {
            SummonApi.setPlayerMaxSlots(player, 8);
        }

        int id = SummonApi.builder(player.world)
                .owner(player)
                .definition(SummonPresetKeys.RAM_WISP)
                .spawn();

        if (id < 0) {
            player.sendMessage(new TextComponentString(
                    String.format("§c[BulletAPI]§r ram_wisp 召唤失败，槽位不足：%d/%d",
                            SummonApi.getPlayerUsedSlots(player),
                            SummonApi.getPlayerMaxSlots(player))
            ));
            return;
        }

        player.sendMessage(new TextComponentString(
                String.format("§a[BulletAPI]§r ram_wisp 已召唤，id=%d，触发方式：冲刺且在水中，当前槽位 %d/%d，召唤物总数: §e%d",
                        id,
                        SummonApi.getPlayerUsedSlots(player),
                        SummonApi.getPlayerMaxSlots(player),
                        Battlefield.of(player.world).summons().count())
        ));
    }

    private static void spawnVisualSyncTest(EntityPlayer player) {
        if (player.world.isRemote) {
            return;
        }

        Vec3d basePos = player.getPositionVector().add(0, player.getEyeHeight() + 1.2D, 0);
        BulletHandle bullet = BulletApi.builder(player.world)
                .position(basePos.add(1.5D, 0.0D, 0.0D))
                .velocity(new Vec3d(0, 0, 0))
                .life(240)
                .damage(0.0F)
                .size(0.55f)
                .rendererType("model_json")
                .set("model", "minecraft:furnace")
                .set("variant", "inventory")
                .set("scale", 0.45f)
                .set("scale_" + TEST_RENDER_STATE, 0.78f)
                .set("tint", 0x66CCFF)
                .set("tint_" + TEST_RENDER_STATE, 0xFF7744)
                .set("rot_mode", "fixed")
                .set("yaw", 45.0f)
                .spawnHandle();

        SummonDefinition definition = SummonDefinition.builder("debug_visual_sync")
                .slotCost(0)
                .life(240)
                .damage(0.0f)
                .texture(TEST_TEXTURE)
                .color(0x99FFCC)
                .size(1.2f)
                .rendererType("billboard")
                .syncIntervalTicks(2)
                .set("model", "minecraft:furnace")
                .set("variant", "inventory")
                .set("scale", 0.48f)
                .set("scale_" + TEST_RENDER_STATE, 0.9f)
                .set("tint", 0x99FFCC)
                .set("tint_" + TEST_RENDER_STATE, 0xFF4466)
                .set(RenderStateData.KEY_RENDER_STATE, "")
                .build();

        SummonHandle summon = SummonApi.builder(player.world)
                .owner(player)
                .position(basePos.add(-1.5D, 0.0D, 0.0D))
                .definition(definition)
                .spawnHandle();

        VISUAL_SYNC_TESTS.add(VisualSyncTest.forBullet(player.world, bullet.getId()));
        VISUAL_SYNC_TESTS.add(VisualSyncTest.forSummon(player.world, summon.getId()));

        player.sendMessage(new TextComponentString(
                String.format("§a[BulletAPI]§r 视觉同步测试已触发，bullet=%d（只切 renderState），summon=%d（切 renderState + rendererType）",
                        bullet.getId(), summon.getId())
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
