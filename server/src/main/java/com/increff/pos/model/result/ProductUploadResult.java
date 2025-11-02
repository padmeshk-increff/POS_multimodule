package com.increff.pos.model.result;

import com.increff.pos.entity.Product;
import com.increff.pos.model.data.FailedUploadRow;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductUploadResult {
    private List<Product> successfullyInserted;
    private List<FailedUploadRow> failedRows;
}