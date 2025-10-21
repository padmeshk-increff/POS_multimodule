package com.increff.pos.utils;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.OrderItemData;
import com.increff.pos.model.form.OrderItemForm;
import com.increff.pos.model.form.OrderItemUpdateForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrderItemUtil {


    public static void validate(OrderItem item) throws ApiException {

        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new ApiException("Quantity must be a positive integer.");
        }

        if (item.getSellingPrice() == null || item.getSellingPrice() < 0) {
            throw new ApiException("Selling price cannot be null or negative.");
        }

        if (item.getOrderId() == null) {
            throw new ApiException("Order ID is required for an order item.");
        }
        if (item.getProductId() == null) {
            throw new ApiException("Product ID is required for an order item.");
        }


    }

    public static List<OrderItem> convert(List<OrderItemForm> orderItemForms){
        List<OrderItem> orderItems = new ArrayList<>();
        for(OrderItemForm orderItemForm:orderItemForms){
            orderItems.add(convert(orderItemForm));
        }
        return orderItems;
    }

    public static List<OrderItemData> convert(List<OrderItem> items, Map<Integer, Product> productMap) {
        return items.stream()
                .map(item -> {
                    OrderItemData data = convert(item); // Re-use the simple converter for basic fields
                    Product product = productMap.get(item.getProductId());

                    // If a matching product is found in the map, set the product name
                    if (product != null) {
                        data.setProductName(product.getName());
                    }
                    return data;
                })
                .collect(Collectors.toList());
    }

    public static OrderItem convert(OrderItemForm orderItemForm){
        OrderItem orderItem = new OrderItem();
        orderItem.setQuantity(orderItemForm.getQuantity());
        orderItem.setSellingPrice(orderItemForm.getSellingPrice());
        orderItem.setProductId(orderItemForm.getProductId());
        return orderItem;
    }

    public static OrderItem convert(OrderItemForm orderItemForm,Integer orderId){
        OrderItem orderItem = convert(orderItemForm);
        orderItem.setOrderId(orderId);
        return orderItem;
    }

    public static OrderItem convert(OrderItemUpdateForm orderItemUpdateForm,Integer orderId,Integer itemId){
        OrderItem orderItem  = new OrderItem();
        orderItem.setQuantity(orderItemUpdateForm.getQuantity());
        orderItem.setSellingPrice(orderItemUpdateForm.getSellingPrice());
        orderItem.setOrderId(orderId);
        orderItem.setId(itemId);
        return orderItem;
    }

    public static OrderItemData convert(OrderItem orderItem){
        OrderItemData orderItemData = new OrderItemData();
        orderItemData.setQuantity(orderItem.getQuantity());
        orderItemData.setId(orderItem.getId());
        orderItemData.setSellingPrice(orderItem.getSellingPrice());
        return orderItemData;
    }

    public static List<OrderItemData> convert(List<OrderItem> orderItems, List<String> productNames){
        List<OrderItemData> orderItemData = new ArrayList<>();
        for(int i=0;i<orderItems.size();i++){
            orderItemData.add(convert(orderItems.get(i),productNames.get(i)));
        }
        return orderItemData;
    }

    public static OrderItemData convert(OrderItem orderItem,String productName){
        OrderItemData orderItemData = convert(orderItem);
        orderItemData.setProductName(productName);
        return orderItemData;
    }

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

}
