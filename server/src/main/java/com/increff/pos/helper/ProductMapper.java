package com.increff.pos.helper; // Or your .utils.mapper package

import com.increff.pos.entity.Client;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.PaginationData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.form.ProductForm;
import com.increff.pos.model.result.PaginatedResult;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    Product convert(ProductForm productForm);

    @Mapping(source="product.id",target="id")
    ProductData convert(Product product, Client client, Inventory inventory);

    @Mapping(source = "results", target = "content")
    PaginationData<ProductData> convert(PaginatedResult<Product> paginatedResult,
                                        @Context Map<Integer, Client> clientMap,
                                        @Context Map<Integer, Inventory> inventoryMap);

    List<ProductData> convert(List<Product> products,
                              @Context Map<Integer, Client> clientMap,
                              @Context Map<Integer, Inventory> inventoryMap);

    @Mapping(source = "clientId", target = "clientName", qualifiedByName = "mapClientName")
    @Mapping(source = "id", target = "quantity", qualifiedByName = "mapQuantity")
    ProductData convert(Product product,
                        @Context Map<Integer, Client> clientMap,
                        @Context Map<Integer, Inventory> inventoryMap);


    @Named("mapClientName")
    default String mapClientName(Integer clientId, @Context Map<Integer, Client> clientMap) {
        if (clientMap == null || clientId == null) {
            return null;
        }
        Client client = clientMap.get(clientId);
        return (client != null) ? client.getClientName() : null;
    }

    @Named("mapQuantity")
    default Integer mapQuantity(Integer productId, @Context Map<Integer, Inventory> inventoryMap) {
        if (inventoryMap == null || productId == null) {
            return 0; // Default to 0 if not found
        }
        Inventory inventory = inventoryMap.get(productId);
        return (inventory != null) ? inventory.getQuantity() : 0; // Default to 0
    }
}

