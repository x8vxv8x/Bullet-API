package com.smd.bulletapi.common.summon;

import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.runtime.ISummonActor;
import com.smd.bulletapi.api.summon.SummonCommand;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@PublicApi
public class SummonContext {
    private final SummonManager manager;
    public final World world;
    public final ISummonActor summon;
    public final EntityLivingBase owner;
    public final SummonDefinition definition;
    public final long worldTick;
    private EntityLivingBase target;
    private List<SummonCommand> commands = Collections.emptyList();

    public SummonContext(SummonManager manager, World world, ISummonActor summon,
                         EntityLivingBase owner, SummonDefinition definition,
                         long worldTick, EntityLivingBase target) {
        this.manager = manager;
        this.world = world;
        this.summon = summon;
        this.owner = owner;
        this.definition = definition;
        this.worldTick = worldTick;
        this.target = target;
    }

    public EntityLivingBase getTarget() {
        return target;
    }

    public void setTarget(EntityLivingBase target) {
        this.target = target;
        summon.setTarget(target);
    }

    public Vec3d getOwnerCenter() {
        return owner.getPositionVector().add(0, owner.getEyeHeight() * 0.7, 0);
    }

    public Vec3d getTargetCenter() {
        return target == null ? null : target.getPositionVector().add(0, target.height * 0.6, 0);
    }

    public int getOwnedSummonCount() {
        return Math.max(1, manager.getOwnedSummonCount(owner.getUniqueID()));
    }

    public List<SummonCommand> getCommands() {
        return commands;
    }

    public boolean hasCommand(String commandId) {
        return getCommand(commandId) != null;
    }

    public SummonCommand getCommand(String commandId) {
        if (commandId == null || commands.isEmpty()) return null;
        for (int i = commands.size() - 1; i >= 0; i--) {
            SummonCommand command = commands.get(i);
            if (commandId.equals(command.getCommandId())) {
                return command;
            }
        }
        return null;
    }

    void attachCommands(List<SummonCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            this.commands = Collections.emptyList();
            return;
        }
        this.commands = Collections.unmodifiableList(new ArrayList<>(commands));
    }
}
