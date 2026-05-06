package com.increff.pos.model.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AiInsightData {

    private final String summary;
    private final List<String> recommendations;
}