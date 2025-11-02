package com.increff.pos.utils;

import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.model.result.OrderResult;
import com.increff.pos.model.result.PaginatedResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrderUtil extends BaseUtil{

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

}
