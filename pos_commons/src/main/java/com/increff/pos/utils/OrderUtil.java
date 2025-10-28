package com.increff.pos.utils;

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
import com.increff.pos.model.result.PaginatedResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OrderUtil extends BaseUtil{

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

    public static PaginationData<OrderData> convert(PaginatedResult result) {
        // Convert the list of OrderResult into OrderData objects by re-using the helper method
        List<OrderData> orderDataList = convert(result.getResults());

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
        List<OrderItemData> itemDataList = OrderItemUtil.convert(items, productMap);
        data.setOrderItemDataList(itemDataList);

        return data;
    }

    public static List<OrderData> convert(PaginatedResult<OrderResult> result,Map<Integer,Product> productMap){
        return result.getResults().stream()
                .map(orderResult -> convert(orderResult.getOrder(), orderResult.getOrderItems(), productMap))
                .collect(Collectors.toList());
    }

    public static List<OrderResult> createOrderResults(List<Order> ordersOnPage, Map<Integer, List<OrderItem>> itemsByOrderIdMap) {

        if (ordersOnPage == null || itemsByOrderIdMap == null) {
            return new ArrayList<>();
        }

        return ordersOnPage.stream()
                .map(order -> {
                    OrderResult orderResult = new OrderResult();
                    orderResult.setOrder(order);
                    List<OrderItem> items = itemsByOrderIdMap.getOrDefault(order.getId(), new ArrayList<>());
                    orderResult.setOrderItems(items);
                    return orderResult;
                })
                .collect(Collectors.toList());

    }

    public static List<Integer> getOrderIds(List<Order> orders){
        return orders.stream().map(Order::getId).collect(Collectors.toList());
    }

    public static List<Integer> getProductIds(PaginatedResult<OrderResult> result){
        return result.getResults().stream()
                .flatMap(res -> res.getOrderItems().stream())
                .map(OrderItem::getProductId)
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Integer> getProductIds(OrderResult orderResult){
        return orderResult.getOrderItems().stream()
                .map(OrderItem::getProductId)
                .distinct()
                .collect(Collectors.toList());
    }

    public static Map<Integer,Product> mapByProductId(List<Product> products){
        return products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }
}
