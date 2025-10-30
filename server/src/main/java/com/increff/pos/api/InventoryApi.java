package com.increff.pos.api;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dao.InventoryDao;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.FailedInventoryUploadRow;
import com.increff.pos.model.data.InventoryUploadRow;
import com.increff.pos.model.result.InventoryReportResult;
import com.increff.pos.model.result.InventoryUploadResult;
import com.increff.pos.utils.InventoryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = ApiException.class)
public class InventoryApi extends AbstractApi{

    @Autowired
    private InventoryDao inventoryDao;

    public Inventory insert(Inventory inventory) throws ApiException{
        checkNull(inventory,"Inventory cannot be null");

        Inventory existingInventory = inventoryDao.selectById(inventory.getProductId());
        checkNotNull(existingInventory,"inventory already exists");

        inventoryDao.insert(inventory);
        return inventory;
    }

    public List<Inventory> getByProductIds(List<Integer> ids){
        return inventoryDao.selectByProductIds(ids);
    }

    public Inventory getCheckByProductId(Integer id) throws ApiException{
        checkNull(id,"Id cannot be null");

        Inventory existingInventory = inventoryDao.selectByProductId(id);
        checkNull(existingInventory,"Inventory doesn't exist");
        return existingInventory;
    }

    public Inventory getCheckById(Integer id) throws ApiException{
        checkNull(id,"Id cannot be null");

        Inventory existingInventory = inventoryDao.selectById(id);
        checkNull(existingInventory,"Inventory doesn't exist");
        return existingInventory;
    }

    public List<Inventory> getAll(){
        return inventoryDao.selectAll();
    }

    public InventoryUploadResult upload(List<InventoryUploadRow> candidateRows, Map<String, Product> productMap) {

        List<Inventory> inventoriesToUpdate = new ArrayList<>();
        List<FailedInventoryUploadRow> failedRows = new ArrayList<>();

        // --- Pre-processing Pass to Find In-File Duplicates ---
        Set<String> duplicateBarcodesInFile = InventoryUtil.findDuplicateBarcodes(candidateRows);

        // --- In-Memory Validation Loop ---
        for (InventoryUploadRow row : candidateRows) {
            try {
                Inventory inventory = InventoryUtil.validateAndConvert(row, productMap, duplicateBarcodesInFile);
                inventoriesToUpdate.add(inventory);
            } catch (ApiException e) {
                FailedInventoryUploadRow fail = new FailedInventoryUploadRow();
                fail.setRow(row);
                fail.setErrorMessage(e.getMessage());
                failedRows.add(fail);
            }
        }

        // --- High-Performance Bulk Update ---
        if (!inventoriesToUpdate.isEmpty()) {
            inventoryDao.bulkUpdate(inventoriesToUpdate);
        }

        InventoryUploadResult result = new InventoryUploadResult();
        result.setSuccessfullyUpdated(inventoriesToUpdate);
        result.setFailedRows(failedRows);
        return result;
    }

    public void initializeInventory(List<Product> newProducts) throws ApiException {
        // 1. Convert the list of Product entities into a list of Inventory entities.
        List<Inventory> newInventories = newProducts.stream()
                .map(product -> {
                    Inventory inventory = new Inventory();
                    inventory.setProductId(product.getId()); // Use the new product's ID
                    inventory.setQuantity(0); // Set the default quantity to 0
                    return inventory;
                })
                .collect(Collectors.toList());

        // 2. Use the highly efficient, generic bulk-insert method from the DAO.
        if (!newInventories.isEmpty()) {
            inventoryDao.insertAll(newInventories);
        }
    }

    public List<Inventory> getLowStockItems(Integer threshold) throws ApiException {
        checkNull(threshold,"Threshold cannot be null");

        return inventoryDao.selectLowStockItems(threshold);
    }

    public List<InventoryReportResult> getInventoryReportData() {
        return inventoryDao.findInventoryReportData();
    }

    public void update(Inventory inventory) throws ApiException{
        checkNull(inventory,"Inventory cannot be null");

        Inventory existingInventory = getCheckByProductId(inventory.getProductId());
        existingInventory.setQuantity(inventory.getQuantity());

        inventoryDao.update(existingInventory);
    }

    public Inventory updateById(Integer id,Inventory inventory) throws ApiException{
        checkNull(id,"Id cannot be null");
        checkNull(inventory,"Inventory cannot be null");

        Inventory existingInventory = inventoryDao.selectById(id);
        checkNull(existingInventory,"Inventory doesn't exist");

        existingInventory.setQuantity(inventory.getQuantity());

        inventoryDao.update(existingInventory);
        return existingInventory;
    }

    public void deleteById(Integer id) throws ApiException{
        checkNull(id,"Id cannot be null");

        Inventory existingInventory = inventoryDao.selectById(id);
        checkNull(existingInventory,"Inventory doesn't exist");

        inventoryDao.deleteById(id);
    }

    public void updateQuantityByProductId(Integer productId, Integer oldQuantity, Integer newQuantity) throws ApiException{
        checkNull(productId,"Product Id cannot be null");
        checkNull(oldQuantity,"Old quantity cannot be null");
        checkNull(newQuantity,"New Quantity cannot be null");

        Inventory inventory = getCheckByProductId(productId);
        Integer quantityAddOn = oldQuantity-newQuantity;
        if(inventory.getQuantity() + quantityAddOn < 0){
            throw new ApiException("Not enough items in stock");
        }

        Integer updatedQuantity = inventory.getQuantity() + quantityAddOn;
        inventory.setQuantity(updatedQuantity);

        inventoryDao.update(inventory);
    }

    public Inventory updateByProductId(Integer productId, Inventory inventoryPojo) throws ApiException{
        checkNull(productId,"Product id cannot be null");
        checkNull(inventoryPojo,"Inventory pojo cannot be null");

        Inventory exisitingInventory = getCheckByProductId(productId);
        checkNull(exisitingInventory,"Inventory doesn't exist");

        exisitingInventory.setQuantity(inventoryPojo.getQuantity());

        inventoryDao.update(exisitingInventory);
        return exisitingInventory;
    }

    public List<Inventory> getByIds(List<Integer> Ids) throws ApiException{
        checkNull(Ids,"Ids cannot be null");

        return inventoryDao.selectByIds(Ids);
    }
}
