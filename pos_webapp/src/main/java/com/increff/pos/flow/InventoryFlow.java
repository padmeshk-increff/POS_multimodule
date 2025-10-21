package com.increff.pos.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.InventoryUploadRow;
import com.increff.pos.model.result.ConversionResult;
import com.increff.pos.model.result.InventoryUploadResult;
import com.increff.pos.utils.InventoryUtil;
import com.increff.pos.utils.TsvUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Transactional(rollbackFor = ApiException.class)
public class InventoryFlow {

    @Autowired
    private InventoryApi inventoryApi;

    @Autowired
    private ProductApi productApi;

    public byte[] uploadByFile(ConversionResult<String[]> tsvResult) throws ApiException {
        // --- Step 1: Convert raw string arrays to structured DTOs ---
        ConversionResult<InventoryUploadRow> conversionResult = InventoryUtil.convertRows(tsvResult);
        List<InventoryUploadRow> candidateRows = conversionResult.getValidRows();
        List<String> initialErrors = conversionResult.getErrors();

        // --- Step 2: High-Performance Bulk Lookup for Product data ---
        Set<String> barcodesInFile = candidateRows.stream()
                .map(p -> p.getBarcode().trim().toLowerCase())
                .collect(Collectors.toSet());

        Map<String, Product> productMap = productApi.getByBarcodes(new ArrayList<>(barcodesInFile))
                .stream().collect(Collectors.toMap(Product::getBarcode, Function.identity()));

        // --- Step 3: Pass to the API layer for business logic processing ---
        InventoryUploadResult uploadResult = inventoryApi.upload(candidateRows, productMap);

        // --- Step 4: Generate the final TSV report file ---
        return TsvUtil.createInventoryUploadReport(uploadResult, candidateRows, initialErrors);
    }

}
