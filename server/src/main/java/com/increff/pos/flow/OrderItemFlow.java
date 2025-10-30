package com.increff.pos.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.OrderItemApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.OrderItemData;
import com.increff.pos.model.enums.OrderStatus;
import com.increff.pos.utils.OrderItemUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@Transactional(rollbackFor = ApiException.class)
public class OrderItemFlow {

    @Autowired
    private OrderApi orderApi;

    @Autowired
    private OrderItemApi orderItemApi;

    @Autowired
    private ProductApi productApi;

    @Autowired
    private InventoryApi inventoryApi;

    public OrderItem add(OrderItem orderItem) throws ApiException{
        checkOrderIsMutable(orderItem.getOrderId());

        Product product = productApi.getCheckById(orderItem.getProductId());
        if (orderItem.getSellingPrice() > product.getMrp()) {
            throw new ApiException("Selling price for product '" + product.getName() + "' cannot be greater than its MRP: " + product.getMrp());
        }

        inventoryApi.updateQuantityByProductId(orderItem.getProductId(),0,orderItem.getQuantity());
        orderApi.updateAmountById(orderItem.getOrderId(),0.00,orderItem.getSellingPrice());
        return orderItemApi.insert(orderItem);
    }

    public OrderItem update(OrderItem orderItem) throws ApiException{
        checkOrderIsMutable(orderItem.getOrderId());

        OrderItem existingOrderItem = orderItemApi.getCheckById(orderItem.getId());

        Product product = productApi.getCheckById(existingOrderItem.getProductId());
        if (orderItem.getSellingPrice() > product.getMrp()) {
            throw new ApiException("Selling price for product '" + product.getName() + "' cannot be greater than its MRP: " + product.getMrp());
        }

        inventoryApi.updateQuantityByProductId(existingOrderItem.getProductId(), existingOrderItem.getQuantity(), orderItem.getQuantity());
        orderApi.updateAmountById(orderItem.getOrderId(), existingOrderItem.getSellingPrice(),orderItem.getSellingPrice());
        return orderItemApi.update(orderItem);
    }

    public void deleteById(Integer orderId,Integer itemId) throws ApiException{
        checkOrderIsMutable(orderId);
        OrderItem orderItem = orderItemApi.getCheckById(itemId);
        inventoryApi.updateQuantityByProductId(orderItem.getProductId(), orderItem.getQuantity(), 0);
        orderApi.updateAmountById(orderId,orderItem.getSellingPrice(),0.00);
        orderItemApi.deleteById(itemId,orderId);
    }

    private void checkOrderIsMutable(Integer orderId) throws ApiException {
        Order order = orderApi.getCheckById(orderId);
        if (order.getOrderStatus() == OrderStatus.INVOICED || order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new ApiException("Cannot modify an order that is already " + order.getOrderStatus());
        }
    }
}
