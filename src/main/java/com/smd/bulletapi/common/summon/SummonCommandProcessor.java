package com.smd.bulletapi.common.summon;

import com.smd.bulletapi.api.summon.SummonCommand;
import com.smd.bulletapi.event.lifecycle.LifecycleRemoveReason;
import com.smd.bulletapi.server.summon.SummonBullet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

final class SummonCommandProcessor {
    private SummonCommandProcessor() {}

    static boolean isStandardCommand(String commandId) {
        return SummonCommand.CLEAR_TARGET.equals(commandId)
                || SummonCommand.FORCE_RETARGET.equals(commandId)
                || SummonCommand.FORCE_TARGET.equals(commandId)
                || SummonCommand.RETURN_TO_OWNER.equals(commandId)
                || SummonCommand.DESPAWN.equals(commandId);
    }

    static boolean applyStandardCommand(SummonManager manager, World world, SummonBullet summon,
                                        SummonContext context, SummonCommand command) {
        String commandId = command.getCommandId();
        if (SummonCommand.CLEAR_TARGET.equals(commandId)) {
            context.setTarget(null);
            summon.resetRetargetCooldown();
            return true;
        }
        if (SummonCommand.FORCE_RETARGET.equals(commandId)) {
            if (summon.getDefinition().getTargetSelector() != null) {
                context.setTarget(summon.getDefinition().getTargetSelector().selectTarget(context));
            } else {
                context.setTarget(null);
            }
            summon.resetRetargetCooldown();
            return true;
        }
        if (SummonCommand.FORCE_TARGET.equals(commandId)) {
            int targetEntityId = command.getInt(SummonCommand.KEY_TARGET_ENTITY_ID, -1);
            Entity entity = targetEntityId < 0 ? null : world.getEntityByID(targetEntityId);
            context.setTarget(entity instanceof EntityLivingBase && !entity.isDead ? (EntityLivingBase) entity : null);
            summon.resetRetargetCooldown();
            return true;
        }
        if (SummonCommand.RETURN_TO_OWNER.equals(commandId)) {
            context.setTarget(null);
            summon.resetRetargetCooldown();
            summon.setState(SummonState.RETURNING);
            return true;
        }
        if (SummonCommand.DESPAWN.equals(commandId)) {
            manager.removeSummon(world, summon.getId(), LifecycleRemoveReason.API_REQUEST);
            return false;
        }
        return true;
    }
}
