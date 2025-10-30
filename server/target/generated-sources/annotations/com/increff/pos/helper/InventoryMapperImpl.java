package com.increff.pos.helper;

import com.increff.pos.entity.Inventory;
import com.increff.pos.model.data.InventoryData;
import com.increff.pos.model.form.InventoryForm;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-30T08:41:21+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 1.8.0_462 (Amazon.com Inc.)"
)
@Component
public class InventoryMapperImpl implements InventoryMapper {

    @Override
    public Inventory convert(InventoryForm inventoryForm) {
        if ( inventoryForm == null ) {
            return null;
        }

        Inventory inventory = new Inventory();

        inventory.setQuantity( inventoryForm.getQuantity() );

        return inventory;
    }

    @Override
    public InventoryData convert(Inventory inventory) {
        if ( inventory == null ) {
            return null;
        }

        InventoryData inventoryData = new InventoryData();

        inventoryData.setId( inventory.getId() );
        inventoryData.setProductId( inventory.getProductId() );
        inventoryData.setQuantity( inventory.getQuantity() );

        return inventoryData;
    }

    @Override
    public List<InventoryData> convert(List<Inventory> inventoryList) {
        if ( inventoryList == null ) {
            return null;
        }

        List<InventoryData> list = new ArrayList<InventoryData>( inventoryList.size() );
        for ( Inventory inventory : inventoryList ) {
            list.add( convert( inventory ) );
        }

        return list;
    }
}
