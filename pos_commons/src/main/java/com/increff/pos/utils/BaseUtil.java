package com.increff.pos.utils;

import com.increff.pos.model.result.PaginatedResult;

import java.util.ArrayList;

public class BaseUtil {

    public static <T> PaginatedResult<T> createEmptyResult(){
        PaginatedResult<T> emptyResult = new PaginatedResult<>();
        emptyResult.setResults(new ArrayList<>());
        emptyResult.setTotalElements(0L);
        emptyResult.setTotalPages(0);
        return emptyResult;
    }

}
