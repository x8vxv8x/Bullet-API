package com.smd.bulletapi.event;

import com.smd.bulletapi.BulletAPI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
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

        Vec3d eyePos = player.getPositionEyes(1.0F);
        Vec3d lookVec = player.getLookVec();

        Vec3d spawnPos = eyePos.add(lookVec.scale(2.0));

        // 弹幕速度：沿玩家视线方向，每 tick 移动 0.3 格
        Vec3d velocity = lookVec.scale(0.3);

        // 弹幕参数：生命 200 tick（10秒），伤害 1.0
        BulletAPI.spawnBullet(player.world, spawnPos, velocity, 200, 1.0F);
    }
}
