package com.increff.pos.api;

import com.increff.pos.commons.exception.ApiException;

public abstract class AbstractApi {
    protected <T> void checkNull(T entity, String message) throws ApiException {
        if (entity == null) {
            throw new ApiException(message);
        }
    }

    protected <T> void checkNotNull(T entity, String message) throws ApiException {
        if (entity != null) {
            throw new ApiException(message);
        }
    }
}
