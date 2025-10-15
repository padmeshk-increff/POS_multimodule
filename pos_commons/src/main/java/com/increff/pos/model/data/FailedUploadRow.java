package com.increff.pos.model.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FailedUploadRow {
    private ProductUploadRow row;
    private String errorMessage;
}
