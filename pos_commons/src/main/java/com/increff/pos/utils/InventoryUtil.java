package com.increff.pos.utils;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.InventoryData;
import com.increff.pos.model.data.InventoryUploadRow;
import com.increff.pos.model.form.InventoryForm;
import com.increff.pos.model.result.ConversionResult;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InventoryUtil extends BaseUtil{

    public static ConversionResult<InventoryUploadRow> convertRows(ConversionResult<String[]> tsvResult) {
        List<InventoryUploadRow> validRows = new ArrayList<>();
        List<String> errors = new ArrayList<>(tsvResult.getErrors());
        int rowNumber = 1;

        for (String[] tokens : tsvResult.getValidRows()) {
            rowNumber++;
            try {
                InventoryUploadRow row = new InventoryUploadRow();
                row.setRowNumber(rowNumber);
                row.setBarcode(tokens[0]);
                row.setQuantity(tokens[1]);

                // Perform a quick check on the quantity format here.
                Integer.parseInt(row.getQuantity().trim());
                validRows.add(row);

            } catch (NumberFormatException e) {
                errors.add("Error in row #" + rowNumber + ": Invalid number format for quantity: '" + tokens[1] + "'");
            }
        }

        ConversionResult<InventoryUploadRow> result = new ConversionResult<>();
        result.setValidRows(validRows);
        result.setErrors(errors);
        return result;
    }

    public static Inventory validateAndConvert(InventoryUploadRow row, Map<String, Product> productMap, Set<String> duplicateBarcodesInFile) throws ApiException {
        String barcode = row.getBarcode().trim().toLowerCase();
        int quantity;

        // --- Perform All Business Validations ---
        if (barcode.isEmpty()) {
            throw new ApiException("Barcode cannot be empty.");
        }
        if (duplicateBarcodesInFile.contains(barcode)) {
            throw new ApiException("Duplicate barcode '" + barcode + "' found within the file. All entries with this barcode are rejected.");
        }
        if (!productMap.containsKey(barcode)) {
            throw new ApiException("Product with barcode '" + row.getBarcode() + "' does not exist.");
        }
        try {
            quantity = Integer.parseInt(row.getQuantity().trim());
            if (quantity < 0) {
                throw new ApiException("Quantity cannot be negative.");
            }
        } catch (NumberFormatException e) {
            throw new ApiException("Invalid number format for quantity: '" + row.getQuantity() + "'");
        }

        // --- If all validations pass, create the Inventory entity ---
        Inventory inv = new Inventory();
        inv.setProductId(productMap.get(barcode).getId());
        inv.setQuantity(quantity);
        return inv;
    }

    public static Set<String> findDuplicateBarcodes(List<InventoryUploadRow> rows) {
        return rows.stream()
                .map(row -> row.getBarcode().trim().toLowerCase())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public static Inventory convert(InventoryForm inventoryForm){
        Inventory inventoryPojo = new Inventory();
        inventoryPojo.setQuantity(inventoryForm.getQuantity());
        return inventoryPojo;
    }

    public static List<InventoryData> convert(List<Inventory> inventoryPojo){
        List<InventoryData> inventoryData = new ArrayList<>();
        for(Inventory inventory:inventoryPojo){
            inventoryData.add(convert(inventory));
        }
        return inventoryData;
    }

    public static InventoryData convert(Inventory inventoryPojo){
        InventoryData inventoryData = new InventoryData();
        inventoryData.setId(inventoryPojo.getId());
        inventoryData.setQuantity(inventoryPojo.getQuantity());
        inventoryData.setProductId(inventoryPojo.getProductId());
        return inventoryData;
    }

    public static Set<String> getBarcodes(List<InventoryUploadRow> rows) {
        if (rows == null) {
            return Collections.emptySet();
        }
        return rows.stream()
                .map(InventoryUploadRow::getBarcode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static Map<String, Product> mapProductsByBarcode(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyMap();
        }
        return products.stream()
                .filter(Objects::nonNull) // Ensure no null products in the list
                .filter(p -> p.getBarcode() != null) // Ensure barcode isn't null
                .collect(Collectors.toMap(
                        // Ensure key is lowercase and handle potential null barcode defensively
                        product -> product.getBarcode().trim(),
                        Function.identity(),
                        (existing, replacement) -> existing // Handle potential duplicate barcodes if needed
                ));
    }
}
