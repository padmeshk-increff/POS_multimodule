package com.increff.pos.model.result;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PaginatedResult<T> {

    private List<T> results;
    private Integer totalPages;
    private Long totalElements;

}
