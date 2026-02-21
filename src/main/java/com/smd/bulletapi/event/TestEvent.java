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
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class TestEvent {

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity().world.isRemote) return;
        if (!(event.getEntity() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntity();

        // ----- 1. 基础属性：位置与速度 -----
        Vec3d eyePos = player.getPositionEyes(1.0F);
        Vec3d lookVec = player.getLookVec();
        Vec3d velocity = lookVec.scale(0.3);

        // ----- 2. 纹理路径（可为null，客户端将根据纹理有无自动选择渲染器）-----
        String texturePath = "bulletapi:textures/entity/bullet.png";

        // ----- 3. 自定义数据：使用建造者的快捷方法设置颜色、尺寸、渲染器类型 -----
        // 注意：NBTTagCompound 不再需要手动创建，除非有额外自定义字段

        int count = 10000;               // 弹幕数量
        int lifeTime = 300;               // 生命周期 15秒
        float damage = 0.0F;

        // ----- 4. 碰撞盒：半径2.0米的球体 -----
        ICollisionShape collisionShape = new SphereShape(2.0);

        Vec3d center = player.getPositionVector().add(0, player.getEyeHeight() + 2.0, 0);
        double halfExtent = 5.0; // 立方体半边长

        // ----- 5. 碰撞回调：命中玩家时执行自定义逻辑 -----
        java.util.function.Consumer<CollisionContext> onCollision = ctx -> {
            ctx.damage = 3.0F; // 修改伤害值

            ctx.world.playSound(null, ctx.hitEntity.posX, ctx.hitEntity.posY, ctx.hitEntity.posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8F, 1.2F);

            ctx.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                    ctx.hitEntity.posX, ctx.hitEntity.posY + 0.5, ctx.hitEntity.posZ,
                    0, 0, 0);

            // 弹幕命中后立即消失
            BulletAPI.removeBullet(ctx.world, ctx.bullet.getId());
        };

        // ----- 6. 生成弹幕（使用建造者模式）-----
        for (int i = 0; i < count; i++) {
            double dx = (Math.random() * 2 - 1) * halfExtent;
            double dy = (Math.random() * 2 - 1) * halfExtent;
            double dz = (Math.random() * 2 - 1) * halfExtent;
            Vec3d spawnPos = center.add(dx, dy, dz);

            BulletAPI.bullet(player.world)
                    .position(spawnPos)
                    .velocity(velocity)
                    .life(lifeTime)
                    .damage(damage)
                    .texture(texturePath)
                    .color(0xFF3333)          // 自动写入 customData
                    .size(2.0f)                // 自动写入 customData
                    .rendererType("billboard") // 自动写入 customData
                    .collisionShape(collisionShape)
                    .onCollision(onCollision)
                    .spawn();                   // 生成弹幕，返回ID（此处未使用）
        }

        int totalBullets = DanmakuManager.getInstance().getBulletCount(player.world);
        player.sendMessage(new TextComponentString(
                String.format("§a[BulletAPI]§r 已生成 %d 枚弹幕，当前世界弹幕总数: §e%d", count, totalBullets)
        ));
    }
}