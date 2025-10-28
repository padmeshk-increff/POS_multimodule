package com.increff.pos.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.InventoryUploadRow;
import com.increff.pos.model.result.ConversionResult;
import com.increff.pos.model.result.InventoryUploadResult;
import com.increff.pos.utils.InventoryUtil;
import com.increff.pos.utils.TsvUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@Transactional(rollbackFor = ApiException.class)
public class InventoryFlow {

    @Autowired
    private InventoryApi inventoryApi;

    @Autowired
    private ProductApi productApi;

    public byte[] uploadByFile(ConversionResult<String[]> tsvResult) throws ApiException {
        ConversionResult<InventoryUploadRow> conversionResult = InventoryUtil.convertRows(tsvResult);
        List<InventoryUploadRow> candidateRows = conversionResult.getValidRows();
        List<String> initialErrors = conversionResult.getErrors();

        Set<String> barcodesInFile = InventoryUtil.getBarcodes(candidateRows);

        List<Product> products = productApi.getByBarcodes(new ArrayList<>(barcodesInFile));
        Map<String, Product> productMap = InventoryUtil.mapProductsByBarcode(products);

        InventoryUploadResult uploadResult = inventoryApi.upload(candidateRows, productMap);

        return TsvUtil.createInventoryUploadReport(uploadResult, candidateRows, initialErrors);
    }

}
