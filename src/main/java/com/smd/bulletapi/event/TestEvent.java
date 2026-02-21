package com.smd.bulletapi.event;

import com.smd.bulletapi.BulletAPI;
import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.DanmakuManager;
import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.common.collision.SphereShape;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class TestEvent {

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        // 仅服务端执行，避免客户端重复生成
        if (event.getEntity().world.isRemote) return;
        if (!(event.getEntity() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntity();

        // ----- 1. 基础属性：位置与速度 -----
        Vec3d eyePos = player.getPositionEyes(1.0F);
        Vec3d lookVec = player.getLookVec();
        Vec3d velocity = lookVec.scale(0.3);

        // ----- 2. 纹理：使用自定义资源路径（请确保实际有此纹理）-----
        String texturePath = "bulletapi:textures/entity/bullet.png"; // 若纹理不存在，将回退为纯色点精灵

        // ----- 3. 自定义数据（NBTTagCompound）：用于客户端渲染控制 -----
        NBTTagCompound customData = new NBTTagCompound();
        customData.setInteger("Color", 0xFF3333);   // 红色叠加
        customData.setFloat("Size", 2.0f);          // 渲染尺寸0.6格
        customData.setBoolean("Glow", true);        // 可选：是否需要发光效果（需渲染器支持）

        int count = 10000;               // 弹幕数量
        int lifeTime = 300;            // 生命周期 15秒
        float damage = 0.0F;

        // ----- 4. 碰撞盒：半径1.0米的球体 -----
        ICollisionShape collisionShape = new SphereShape(2.0);

        Vec3d center = player.getPositionVector().add(0, player.getEyeHeight() + 2.0, 0);

        // 立方体半边长（边长 = 10 格）
        double halfExtent = 5.0;

        // ----- 5. 碰撞回调：命中玩家时执行自定义逻辑 -----
        java.util.function.Consumer<CollisionContext> onCollision = ctx -> {
            // 修改伤害值（原本为1.0F，现改为3.0F）
            ctx.damage = 3.0F;

            // 播放爆炸音效
            ctx.world.playSound(null, ctx.hitEntity.posX, ctx.hitEntity.posY, ctx.hitEntity.posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8F, 1.2F);

            // 生成爆炸粒子（会自动同步给附近玩家）
            ctx.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                    ctx.hitEntity.posX, ctx.hitEntity.posY + 0.5, ctx.hitEntity.posZ,
                    0, 0, 0);

            // 弹幕命中后立即消失（移除自身）
            BulletAPI.removeBullet(ctx.world, ctx.bullet.getId());
        };

        // ----- 6. 生成弹幕（完整参数版）-----
        for (int i = 0; i < count; i++) {
            // 立方体内随机均匀分布
            double dx = (Math.random() * 2 - 1) * halfExtent;
            double dy = (Math.random() * 2 - 1) * halfExtent;
            double dz = (Math.random() * 2 - 1) * halfExtent;

            Vec3d spawnPos = center.add(dx, dy, dz);

            // 使用完整参数 API，但碰撞盒与回调均设为 null（纯视觉测试）
            BulletAPI.spawnBullet(
                    player.world,
                    spawnPos,
                    velocity,
                    lifeTime,
                    damage,
                    texturePath,
                    customData,
                    collisionShape,   // 无碰撞盒
                    onCollision   // 无碰撞回调
            );
        }

        int totalBullets = DanmakuManager.getInstance().getBulletCount(player.world);
        player.sendMessage(new TextComponentString(
                String.format("§a[BulletAPI]§r 已生成 %d 枚弹幕，当前世界弹幕总数: §e%d", count, totalBullets)
        ));
    }
}