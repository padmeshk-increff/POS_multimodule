package com.increff.pos.helper; // Or your .utils.mapper package

import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.OrderItemData;
import com.increff.pos.model.form.OrderItemForm;
import com.increff.pos.model.form.OrderItemUpdateForm;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Map;

/**
 * Replaces the conversion methods in OrderItemUtil.java based on
 * the methods used in OrderItemDto.
 * componentModel = "spring" creates a Spring Bean to be @Autowired.
 */
@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    /**
     * Replaces OrderItemUtil.convert(OrderItemForm orderItemForm, Integer orderId)
     * Used in: OrderItemDto.add()
     */
    @Mapping(target = "orderId", source = "orderId")
    OrderItem convert(OrderItemForm form, Integer orderId);

    List<OrderItem> convert(List<OrderItemForm> orderItemForms);
    /**
     * Replaces OrderItemUtil.convert(OrderItemUpdateForm ..., Integer orderId, Integer itemId)
     * Used in: OrderItemDto.updateById()
     */
    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "id", source = "itemId")
    OrderItem convert(OrderItemUpdateForm orderItemUpdateForm, Integer orderId, Integer itemId);

    /**
     * Replaces OrderItemUtil.convert(OrderItem orderItem, Product product)
     * Used in: OrderItemDto.add() and OrderItemDto.updateById()
     * MapStruct automatically maps fields with the same name.
     */
    @Mapping(source = "orderItem.id", target = "id")
    @Mapping(source = "product.name", target = "productName")
    OrderItemData convert(OrderItem orderItem, Product product);

    /**
     * Replaces the logic from OrderItemUtil.convert(List<OrderItem> items, List<Product> products)
     * with the safer Map-based implementation.
     * Used in: OrderItemDto.getAll() and OrderItemDto.getByOrderId()
     */
    @Named("withProductMap")
    List<OrderItemData> convert(List<OrderItem> items, @Context Map<Integer, Product> productMap);

    /**
     * Helper for the method above. Converts a single item.
     * This is the core logic from your OrderItemUtil.convert(List<OrderItem>, Map) stream.
     * @Mapping tells MapStruct to use our custom "mapProductName" method.
     */
    @Mapping(target = "productName", source = "orderItem", qualifiedByName = "mapProductName")
    OrderItemData convert(OrderItem orderItem, @Context Map<Integer, Product> productMap);

    /**
     * This is our custom logic, which MapStruct will call.
     * It finds the product in the map and returns its name.
     */
    @Named("mapProductName")
    default String mapProductName(OrderItem item, @Context Map<Integer, Product> productMap) {
        if (productMap == null || item == null) {
            return null;
        }
        Product product = productMap.get(item.getProductId());
        // Use "Unknown Product" as a fallback if product not found
        return (product != null) ? product.getName() : "Unknown Product";
    }

}

