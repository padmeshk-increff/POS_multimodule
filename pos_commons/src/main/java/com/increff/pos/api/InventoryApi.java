package com.increff.pos.api;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dao.InventoryDao;
import com.increff.pos.entity.Inventory;
import com.increff.pos.utils.InventoryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public void upload(List<Inventory> inventoryList, List<String> errors) throws ApiException {
        InventoryUtil.validateDuplicates(inventoryList);

        for (Inventory inventory : inventoryList) {
            try {
                update(inventory);
            } catch (ApiException e) {
                errors.add(e.getMessage());
            }
        }
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

    public void updateQuantityById(Integer productId,Integer oldQuantity,Integer newQuantity) throws ApiException{
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

}
