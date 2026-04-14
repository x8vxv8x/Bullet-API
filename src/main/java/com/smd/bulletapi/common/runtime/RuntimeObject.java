package com.smd.bulletapi.common.runtime;

import com.smd.bulletapi.api.annotation.InternalApi;

/**
 * 运行时对象的最小公共协议，只暴露 world store 需要的身份和存活信息。
 */
@InternalApi
public interface RuntimeObject {
    int getId();

    boolean isDead();
}
