package com.increff.pos.utils;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.OrderItemData;
import com.increff.pos.model.form.OrderItemForm;
import com.increff.pos.model.form.OrderItemUpdateForm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrderItemUtil {

    public static void setOrderId(List<OrderItem> orderItems,Integer orderId){
        for(OrderItem orderItem:orderItems){
            orderItem.setOrderId(orderId);
        }
    }

    public static Double calculateTotalAmount(List<OrderItem> orderItems){
        Double amount = 0.00;
        for(OrderItem orderItem:orderItems){
            amount += orderItem.getSellingPrice() * orderItem.getQuantity();
        }
        return amount;
    }

    public static Map<Integer, List<OrderItem>> mapItemsByOrderId(List<OrderItem> orderItems) {
        // Handle null or empty input gracefully
        if (orderItems == null || orderItems.isEmpty()) {
            return Collections.emptyMap();
        }

        // Use stream().collect(Collectors.groupingBy(...)) to create the map
        return orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
    }
}
