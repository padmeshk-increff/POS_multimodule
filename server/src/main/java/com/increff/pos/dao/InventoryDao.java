package com.increff.pos.dao;

import com.increff.pos.entity.Inventory;
import com.increff.pos.model.result.InventoryReportResult;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class InventoryDao extends AbstractDao<Inventory> {
    private static final String SELECT_BY_PRODUCT_ID = "select p from Inventory p where productId = :productId";
    private static final String SELECT_BY_PRODUCT_IDS = "select p from Inventory p where productId in :productIds";
    private static final String SELECT_LOW_STOCK = "SELECT i FROM Inventory i WHERE i.quantity < :threshold ORDER BY i.quantity ASC";
    private static final String FIND_INVENTORY_REPORT_DATA =
            "SELECT NEW com.increff.pos.model.result.InventoryReportResult(" +
                    "   p.id, p.name, p.barcode, p.category, p.mrp, i.quantity" +
                    ") " +
                    "FROM Inventory i JOIN Product p ON i.productId = p.id " +
                    "ORDER BY p.name ASC";

    public List<InventoryReportResult> findInventoryReportData() {
        return getCustomResultList(FIND_INVENTORY_REPORT_DATA, InventoryReportResult.class, null); // No parameters
    }

    public Inventory selectByProductId(Integer productId) {
        TypedQuery<Inventory> query = getQuery(SELECT_BY_PRODUCT_ID);
        query.setParameter("productId", productId);
        return getFirstRowFromQuery(query);
    }

    public List<Inventory> selectByProductIds(List<Integer> productIds) {
        TypedQuery<Inventory> query = getQuery(SELECT_BY_PRODUCT_IDS);
        query.setParameter("productIds", productIds);
        return query.getResultList();
    }

    public List<Inventory> selectLowStockItems(Integer threshold) {
        TypedQuery<Inventory> query = getQuery(SELECT_LOW_STOCK);
        query.setParameter("threshold", threshold);
        return query.getResultList();
    }

    @Transactional
    public void bulkUpdate(List<Inventory> inventoriesToUpdate) {
        // --- Step 1: High-Performance Bulk Fetch ---
        // a. Collect all the product IDs from the list of inventories to be updated.
        List<Integer> productIds = inventoriesToUpdate.stream()
                .map(Inventory::getProductId)
                .collect(Collectors.toList());

        // b. Fetch all existing inventory records from the database in a SINGLE query.
        List<Inventory> existingInventories = selectByProductIds(productIds);

        // c. Create a map for fast, O(1) lookup of an inventory record by its productId.
        Map<Integer, Inventory> existingInventoryMap = existingInventories.stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        // --- Step 2: In-Memory Update ---
        // Loop through the list of updates provided by the business layer.
        for (Inventory updateRequest : inventoriesToUpdate) {
            // Find the corresponding record that we fetched from the database.
            Inventory existingInventory = existingInventoryMap.get(updateRequest.getProductId());

            if (existingInventory != null) {
                // Update the quantity of the existing, managed entity.
                existingInventory.setQuantity(updateRequest.getQuantity());
            }
            // Note: If an inventory record doesn't exist, we silently ignore it,
            // as the business logic layer is responsible for ensuring data integrity.
            // A new inventory record is only created when a new Product is created.
        }

        // --- Step 3: Automatic Batch Update ---
        // Because this method is @Transactional, when it completes successfully,
        // Hibernate's "dirty checking" will automatically detect all the changes
        // made to the 'existingInventory' objects and will send a single,
        // efficient batch of UPDATE statements to the database.
    }
}