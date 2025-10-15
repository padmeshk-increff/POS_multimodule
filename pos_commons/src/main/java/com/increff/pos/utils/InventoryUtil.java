package com.increff.pos.utils;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Inventory;
import com.increff.pos.model.data.InventoryData;
import com.increff.pos.model.form.InventoryForm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InventoryUtil extends BaseUtil{
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

    public static void validate(Inventory inventory) throws ApiException {
        if (inventory.getProductId() == null) {
            throw new ApiException("Product ID cannot be null.");
        }

        if (inventory.getQuantity() == null || inventory.getQuantity() < 0) {
            throw new ApiException("Quantity must be a non-negative integer.");
        }
    }

    public static void validateDuplicates(List<Inventory> inventoryList) throws ApiException {
        Set<Integer> productIdsInFile = new HashSet<>();
        for (Inventory inventory : inventoryList) {
            if (productIdsInFile.contains(inventory.getProductId())) {
                throw new ApiException("Duplicate entry for Product ID #" + inventory.getProductId() + " found in the file.");
            }
            productIdsInFile.add(inventory.getProductId());
        }
    }
}
