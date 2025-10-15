package com.increff.pos.model.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PaginationData<T> {

    private List<T> content;
    private int totalPages;
    private long totalElements;

}
