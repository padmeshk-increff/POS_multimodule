package com.increff.pos.model.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Date;

@Getter
@AllArgsConstructor
public class SalesOverTimeResult {

    // The start of the day (e.g., 2025-10-23T00:00:00Z)
    private Date date;

    // The total revenue for that entire day
    private Double revenue;
}

