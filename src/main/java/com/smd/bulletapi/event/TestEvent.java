package com.smd.bulletapi.event;

import com.smd.bulletapi.BulletAPI;
import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.DanmakuManager;
import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.common.collision.SphereShape;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
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

        double ringRadius = 8.0;
        double heightOffset = player.getEyeHeight() + 2.0;
        String texturePath = "bulletapi:textures/entity/bullet.png";
        int count = 10000;
        int lifeTime = 300;          // 15秒
        float damage = 2.0F;
        ICollisionShape collisionShape = new SphereShape(8.0);

        java.util.function.Consumer<CollisionContext> onCollision = ctx -> {
            ctx.damage = ctx.bullet.getDamage();
            ctx.world.playSound(null, ctx.hitEntity.posX, ctx.hitEntity.posY, ctx.hitEntity.posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8F, 1.2F);
            ctx.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                    ctx.hitEntity.posX, ctx.hitEntity.posY + 0.5, ctx.hitEntity.posZ,
                    0, 0, 0);
            BulletAPI.removeBullet(ctx.world, ctx.bullet.getId());
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
            double speed = 0.8; // 速度大小（格/tick）
            Vec3d initialVel = toTarget.normalize().scale(speed);

            BulletAPI.bullet(player.world)
                    .position(spawnPos)
                    .velocity(initialVel)
                    .life(lifeTime)
                    .damage(damage)
                    .texture(texturePath)
                    .color(0xFF3333)
                    .size(2.0f)
                    .rendererType("billboard")
                    .collisionShape(collisionShape)
                    .onCollision(onCollision)
                    .onlyPlayer(false)
                    .spawn();
        }

        int totalBullets = DanmakuManager.getInstance().getBulletCount(player.world);
        player.sendMessage(new TextComponentString(
                String.format("§a[BulletAPI]§r 已生成 %d 枚直线弹幕，当前世界弹幕总数: §e%d", count, totalBullets)
        ));
    }
}