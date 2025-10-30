package com.increff.pos.helper;

import com.increff.pos.entity.Inventory;
import com.increff.pos.model.data.InventoryData;
import com.increff.pos.model.form.InventoryForm;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    /**
     * Replaces InventoryUtil.convert(InventoryForm inventoryForm)
     * This will automatically map the 'quantity' field.
     */
    Inventory convert(InventoryForm inventoryForm);

    /**
     * Replaces InventoryUtil.convert(Inventory inventoryPojo)
     * This will automatically map id, productId, and quantity.
     */
    InventoryData convert(Inventory inventory);

    /**
     * Replaces InventoryUtil.convert(List<Inventory> inventoryPojo)
     * MapStruct handles the list automatically.
     */
    List<InventoryData> convert(List<Inventory> inventoryList);
}
