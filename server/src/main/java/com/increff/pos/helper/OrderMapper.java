package com.increff.pos.helper; // Or your .utils.mapper package

import com.increff.pos.entity.Order;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.form.OrderForm;
import com.increff.pos.model.form.OrderUpdateForm;
import com.increff.pos.model.result.OrderResult;
import com.increff.pos.model.result.PaginatedResult;
import com.increff.pos.model.data.PaginationData;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;

/**
 * Replaces the conversion methods in OrderUtil.java.
 * uses = {OrderItemMapper.class} lets this mapper delegate work.
 */
@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {

    /**
     * Replaces OrderUtil.convert(OrderForm orderForm)
     */
    @Mapping(target = "orderStatus", constant = "CREATED")
    Order convert(OrderForm orderForm);

    /**
     * Replaces OrderUtil.convert(OrderUpdateForm orderUpdateForm)
     */
    Order convert(OrderUpdateForm orderUpdateForm);


    // --- CONVERSION METHODS (WITH CONTEXT) ---
    // By removing the simple convert methods, we force MapStruct
    // to use these context-aware methods, which solves the bug.

    /**
     * Replaces OrderUtil.convert(PaginatedResult<OrderResult> result, Map<Integer,Product> productMap)
     * This is the method you call from your OrderDto.
     * It will now correctly use the helper methods below that pass the productMap.
     */
    @Mapping(source = "results", target = "content")
    PaginationData<OrderData> convert(PaginatedResult<OrderResult> result, @Context Map<Integer, Product> productMap);

    /**
     * Helper list converter for the method above.
     * MapStruct will automatically call this to convert the List.
     */
    List<OrderData> convert(List<OrderResult> orderResults, @Context Map<Integer, Product> productMap);

    /**
     * This is the helper MapStruct uses for the list above.
     * It maps all the simple Order fields.
     * It correctly passes the productMap to the OrderItemMapper.
     */
    @Mapping(source = "order.id", target = "id")
    @Mapping(source = "order.orderStatus", target = "orderStatus")
    @Mapping(source = "order.customerName", target = "customerName")
    @Mapping(source = "order.customerPhone", target = "customerPhone")
    @Mapping(source = "order.totalAmount", target = "totalAmount")
    @Mapping(source = "order.createdAt", target = "createdAt")
    @Mapping(source = "orderItems", target = "orderItemDataList", qualifiedByName = "withProductMap")
    OrderData convert(OrderResult orderResult, @Context Map<Integer, Product> productMap);
}

