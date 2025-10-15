package com.increff.pos.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Product;
import com.increff.pos.model.result.ConversionResult;
import com.increff.pos.utils.InventoryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
@Transactional(rollbackFor = ApiException.class)
public class InventoryFlow {

    @Autowired
    private InventoryApi inventoryApi;

    @Autowired
    private ProductApi productApi;

    //TODO: break it down into smaller units - util functions
    public ConversionResult<Inventory> convert(MultipartFile file) throws ApiException {
        if (file.isEmpty()) {
            throw new ApiException("File is empty. Please upload a valid TSV file.");
        }

        List<Inventory> validInventoryList = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int rowNumber = 1; // To track the row number for clear error messages

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line = reader.readLine(); // Skip header
            if (line == null) {
                throw new ApiException("File is empty or has no header.");
            }

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                if(line.trim().isEmpty()){
                    continue;
                }

                String[] tokens = line.split("\t");

                try {
                    // The main method now orchestrates the calls to the helper functions.
                    Inventory inventory = processInventoryRow(tokens);
                    validInventoryList.add(inventory);
                } catch (ApiException e) {
                    errors.add("Error in row #" + rowNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new ApiException("Failed to read the uploaded file. Ensure it's a valid TSV.");
        }

        // Prepare the conversion result
        ConversionResult<Inventory> conversionResult = new ConversionResult<>();
        conversionResult.setValidRows(validInventoryList);
        conversionResult.setErrors(errors);
        return conversionResult;
    }

    private Inventory processInventoryRow(String[] tokens) throws ApiException {
        // Step 1: Structural Validation
        if (tokens.length != 2) {
            throw new ApiException("Invalid number of columns. Expected 2, found " + tokens.length);
        }

        // Step 2: Parsing and Data Type Validation
        String barcode;
        int quantity;

        try {
            barcode = tokens[0].trim().toLowerCase();
            quantity = Integer.parseInt(tokens[1].trim());
        } catch (NumberFormatException e) {
            throw new ApiException("Invalid number format for quantity: '" + tokens[1] + "'");
        }

        // Step 3: Business Logic Validation (Product Existence)
        Product product = productApi.getByBarcode(barcode);
        if (product == null) {
            throw new ApiException("Product with barcode '" + barcode + "' does not exist.");
        }

        // Step 4: Entity Creation
        Inventory inventory = new Inventory();
        inventory.setProductId(product.getId());
        inventory.setQuantity(quantity);
        InventoryUtil.validate(inventory);

        return inventory;
    }
}
