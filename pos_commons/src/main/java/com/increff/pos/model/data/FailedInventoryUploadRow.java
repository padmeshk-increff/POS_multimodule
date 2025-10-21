package com.increff.pos.model.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FailedInventoryUploadRow {
    private InventoryUploadRow row;
    private String errorMessage;
}
