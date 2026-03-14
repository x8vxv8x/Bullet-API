package com.smd.bulletapi.debug;

import com.smd.bulletapi.api.BattlefieldQueryApi;
import com.smd.bulletapi.api.BulletApi;
import com.smd.bulletapi.api.LaserApi;
import com.smd.bulletapi.api.SummonApi;
import com.smd.bulletapi.api.builder.BulletBuilder;
import com.smd.bulletapi.api.summon.SummonCommand;
import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.common.collision.SphereShape;
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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class TestEvent {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().world.isRemote) return;
        if (!"arrow".equals(event.getSource().getDamageType())) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        EntityLivingBase victim = event.getEntityLiving();

        if (player.isSneaking() && player.isInWater()) {
            spawnLaserEyeTest(player);
        } else if (player.isSprinting() && player.isInWater()) {
            spawnRamWispTest(player);
        } else if (player.isSprinting()) {
            spawnFairyOrbTest(player);
        } else if (player.isInWater()) {
            spawnLaserPolyTest(player);
        } else if (player.isSneaking()) {
            spawnModelTestBullets(player, victim);
        } else {
            spawnDefaultBillboardTest(player, victim);
        }
    }

    @SubscribeEvent
    public static void onSummonBodyCollision(BulletCollisionEvent event) {
        if (event.getWorld().isRemote) return;
        if (!(event.getHitEntity() instanceof EntityLivingBase)) return;

        CollisionContext ctx = event.getContext();
        if (!ctx.isSummonBody()) return;
        if (ctx.attackSource == null) return;
        if (!SummonPresetKeys.RAM_WISP.equals(ctx.attackSource.getSummonDefinitionId())) return;

        EntityLivingBase hit = (EntityLivingBase) event.getHitEntity();
        int summonId = ctx.attackSource.getSummonId();
        if (summonId >= 0 && SummonApi.supportsCommand(event.getWorld(), summonId, RamStrikeMoveController.COMMAND_BODY_HIT)) {
            SummonApi.sendCommand(event.getWorld(), summonId,
                    SummonCommand.of(RamStrikeMoveController.COMMAND_BODY_HIT)
                            .withInt(RamStrikeMoveController.KEY_HIT_ENTITY_ID, hit.getEntityId()));
        }

        ctx.damage = Math.max(ctx.damage, event.getBullet().getDamage()) * 1.25f;
        event.getWorld().playSound(null, hit.posX, hit.posY, hit.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.45F, 1.35F);
        event.getWorld().spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                hit.posX, hit.posY + hit.height * 0.5D, hit.posZ,
                0.0D, 0.02D, 0.0D);
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
            BulletApi.remove(ctx.world, ctx.bullet.getId());
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

        int totalBullets = BattlefieldQueryApi.getBulletCount(player.world);
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
            BulletApi.remove(ctx.world, ctx.bullet.getId());
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

        int totalBullets = BattlefieldQueryApi.getBulletCount(player.world);
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
        ILaserHitBehavior hitBehavior = ctx -> {
            ctx.damage = ctx.laser.getDamage();
        };

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
        if (player.world.isRemote) return;
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
                        SummonApi.getCount(player.world))
        ));
    }

    private static void spawnLaserEyeTest(EntityPlayer player) {
        if (player.world.isRemote) return;
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
                        SummonApi.getCount(player.world))
        ));
    }

    private static void spawnRamWispTest(EntityPlayer player) {
        if (player.world.isRemote) return;
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
                        SummonApi.getCount(player.world))
        ));
    }
}
