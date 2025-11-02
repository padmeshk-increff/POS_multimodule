package com.increff.pos.model.result;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ConversionResult<T> {

    List<T> validRows;
    List<String> errors;

}
