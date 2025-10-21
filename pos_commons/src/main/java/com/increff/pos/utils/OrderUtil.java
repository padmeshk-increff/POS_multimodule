package com.increff.pos.utils;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.data.OrderItemData;
import com.increff.pos.model.data.PaginationData;
import com.increff.pos.model.enums.OrderStatus;
import com.increff.pos.model.form.OrderForm;
import com.increff.pos.model.form.OrderUpdateForm;
import com.increff.pos.model.result.OrderResult;
import com.increff.pos.model.result.PaginatedOrderResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrderUtil {

    public static Order convert(OrderForm orderForm){
        Order order = new Order();
        order.setCustomerName(orderForm.getCustomerName());
        order.setCustomerPhone(orderForm.getCustomerPhone());
        order.setOrderStatus(OrderStatus.CREATED);
        return order;
    }

    public static Order convert(OrderUpdateForm orderUpdateForm){
        Order order = new Order();
        order.setCustomerPhone(orderUpdateForm.getCustomerPhone());
        order.setCustomerName(orderUpdateForm.getCustomerName());
        order.setOrderStatus(orderUpdateForm.getStatus());
        return order;
    }

    public static OrderData convert(Order order, List<OrderItem> items) {
        OrderData data = new OrderData();
        data.setId(order.getId());
        data.setOrderStatus(order.getOrderStatus());
        data.setCustomerName(order.getCustomerName());
        data.setTotalAmount(order.getTotalAmount());
        data.setCreatedAt(order.getCreatedAt());

        List<OrderItemData> itemDataList = items.stream()
                .map(OrderItemUtil::convert) // Assumes you have an OrderItemUtil helper
                .collect(Collectors.toList());
        data.setOrderItemDataList(itemDataList);

        return data;
    }

    public static OrderData convert(OrderResult orderResult){
        return convert(orderResult.getOrder(), orderResult.getOrderItems());
    }

    public static PaginationData<OrderData> convert(PaginatedOrderResult result) {
        // a. Get the pre-assembled list of OrderResult objects
        List<OrderResult> orderResults = result.getOrderResults();

        // b. Convert each OrderResult into an OrderData object by re-using the helper above
        List<OrderData> orderDataList = convert(orderResults);

        // c. Construct and return the final PaginationData object
        PaginationData<OrderData> paginationData = new PaginationData<>();
        paginationData.setContent(orderDataList);
        paginationData.setTotalPages(result.getTotalPages());
        paginationData.setTotalElements(result.getTotalElements());

        return paginationData;
    }

    public static List<OrderData> convert(List<OrderResult> orderResults) {
        return orderResults.stream()
                .map(result -> convert(result.getOrder(), result.getOrderItems()))
                .collect(Collectors.toList());
    }

    public static OrderData convert(Order order, List<OrderItem> items, Map<Integer, Product> productMap) {
        OrderData data = new OrderData();
        data.setId(order.getId());
        data.setOrderStatus(order.getOrderStatus());
        data.setCustomerPhone(order.getCustomerPhone());
        data.setCustomerName(order.getCustomerName());
        data.setTotalAmount(order.getTotalAmount());
        data.setCreatedAt(order.getCreatedAt());
        // Delegate the item conversion to OrderItemUtil, now passing the productMap
        List<OrderItemData> itemDataList = OrderItemUtil.convert(items, productMap);
        data.setOrderItemDataList(itemDataList);

        return data;
    }
}
