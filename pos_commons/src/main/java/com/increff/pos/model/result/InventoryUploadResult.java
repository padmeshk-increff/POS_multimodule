package com.increff.pos.model.result;

import com.increff.pos.entity.Inventory;
import com.increff.pos.model.data.FailedInventoryUploadRow;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InventoryUploadResult {
    private List<Inventory> successfullyUpdated;
    private List<FailedInventoryUploadRow> failedRows;
}
