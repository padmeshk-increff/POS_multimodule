package com.increff.pos.utils;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Client;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.data.ProductUploadRow;
import com.increff.pos.model.form.ProductForm;
import com.increff.pos.model.result.ConversionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProductUtil extends BaseUtil{

    public static ConversionResult<ProductUploadRow> convertRows(ConversionResult<String[]> tsvResult) {
        List<ProductUploadRow> validRows = new ArrayList<>();
        // Start with any structural errors found by the TsvUtil.
        List<String> errors = new ArrayList<>(tsvResult.getErrors());
        int rowNumber = 1; // Corresponds to the data row number (after header)

        for (String[] tokens : tsvResult.getValidRows()) {
            rowNumber++;
            try {

                ProductUploadRow row = new ProductUploadRow();
                row.setRowNumber(rowNumber);
                row.setBarcode(tokens[0]);
                row.setName(tokens[1]);
                row.setMrp(tokens[2]); // Keep as string for now, but validate format
                row.setClientName(tokens[3]);
                row.setCategory(tokens[4]);

                // Perform a quick check on the MRP format here.
                Double.parseDouble(row.getMrp().trim());

                validRows.add(row);

            } catch (NumberFormatException e) {
                // If MRP is not a valid double, add an error.
                errors.add("Error in row #" + rowNumber + ": Invalid number format for MRP: '" + tokens[2] + "'");
            }
        }

        ConversionResult<ProductUploadRow> result = new ConversionResult<>();
        result.setValidRows(validRows);
        result.setErrors(errors);
        return result;
    }

    public static Set<String> findDuplicateBarcodesInFile(List<ProductUploadRow> rows) {
        return rows.stream()
                // Normalize the barcode for accurate comparison
                .map(row -> row.getBarcode().trim().toLowerCase())
                // Group by barcode and count the occurrences of each
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                // Get the stream of map entries (barcode -> count)
                .entrySet().stream()
                // Filter for any entry where the count is greater than 1
                .filter(entry -> entry.getValue() > 1)
                // Collect the keys (the duplicate barcodes) into a Set
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public static Product validateAndConvert(
            ProductUploadRow row,
            Map<String, Client> clientMap,
            Set<String> existingBarcodesInDb
    ) throws ApiException {

        // --- Normalize Data ---
        String barcode = row.getBarcode().trim().toLowerCase();
        String clientName = row.getClientName().trim().toLowerCase();
        String name = row.getName().trim();
        String category = row.getCategory().trim();

        // --- Perform All Business Validations ---

        // 1. Check for empty barcode or name
        if (barcode.isEmpty()) {
            throw new ApiException("Barcode cannot be empty.");
        }
        if (name.isEmpty()) {
            throw new ApiException("Product name cannot be empty.");
        }

        // 2. Check if barcode already exists in the database
        if (existingBarcodesInDb.contains(barcode)) {
            throw new ApiException("Barcode '" + barcode + "' already exists in the database.");
        }

        // 3. Check if client exists
        if (!clientMap.containsKey(clientName)) {
            throw new ApiException("Client with name '" + row.getClientName() + "' does not exist.");
        }

        // 4. Validate and parse MRP
        Double mrp = Double.parseDouble(row.getMrp().trim());
        if (mrp < 0) {
            throw new ApiException("MRP cannot be negative.");
        }

        // --- If all validations pass, create the Product entity ---
        Product p = new Product();
        p.setBarcode(barcode);
        p.setName(name);
        p.setMrp(mrp);
        p.setClientId(clientMap.get(clientName).getId());
        p.setCategory(category);
        return p;
    }

    public static Product convert(ProductForm productForm){
        Product productPojo = new Product();
        productPojo.setName(productForm.getName());
        productPojo.setCategory(productForm.getCategory());
        productPojo.setMrp(productForm.getMrp());
        productPojo.setImageUrl(productForm.getImageUrl());
        productPojo.setBarcode(productForm.getBarcode());
        productPojo.setClientId(productForm.getClientId());
        return productPojo;
    }

    public static List<ProductData> convert(List<Product> productsPojo){
        List<ProductData> productsData = new ArrayList<>();
        for(Product productPojo:productsPojo){
            productsData.add(convert(productPojo));
        }
        return productsData;
    }

    public static ProductData convert(Product productPojo){
        ProductData productData = new ProductData();
        productData.setId(productPojo.getId());
        productData.setBarcode(productPojo.getBarcode());
        productData.setCategory(productPojo.getCategory());
        productData.setMrp(productPojo.getMrp());
        productData.setName(productPojo.getName());
        productData.setClientId(productPojo.getClientId());
        productData.setImageUrl(productPojo.getImageUrl());
        return productData;
    }

}
