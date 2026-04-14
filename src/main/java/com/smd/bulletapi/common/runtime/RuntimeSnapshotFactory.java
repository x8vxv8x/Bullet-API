package com.smd.bulletapi.common.runtime;

import com.smd.bulletapi.api.annotation.InternalApi;

@InternalApi
public interface RuntimeSnapshotFactory<T extends RuntimeObject, S> {
    S create(T runtime);
}
