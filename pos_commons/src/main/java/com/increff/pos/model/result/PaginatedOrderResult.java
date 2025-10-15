package com.increff.pos.model.result;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
public class PaginatedOrderResult {

    private List<OrderResult> orderResults;
    private int totalPages;
    private long totalElements;

}
