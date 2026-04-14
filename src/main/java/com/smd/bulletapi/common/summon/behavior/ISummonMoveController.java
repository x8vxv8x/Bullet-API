package com.smd.bulletapi.common.summon.behavior;

import com.smd.bulletapi.api.annotation.SpiApi;
import com.smd.bulletapi.common.summon.SummonContext;

@SpiApi
public interface ISummonMoveController {
    /**
     * 没有有效目标时的统一运动入口。
     * 内部必须同时处理超出 leash 后的恢复，以及主人附近的自动巡航。
     */
    void tickNoTargetMovement(SummonContext context);

    /**
     * 存在有效目标且未触发恢复流程时的战斗运动入口。
     */
    void tickCombatMovement(SummonContext context);
}
