package com.increff.pos.dto;

import com.increff.pos.api.OrderItemApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.flow.OrderItemFlow;
import com.increff.pos.model.data.OrderItemData;
import com.increff.pos.model.form.OrderItemForm;
import com.increff.pos.model.form.OrderItemUpdateForm;
import com.increff.pos.utils.OrderItemUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderItemDto extends AbstractDto{

    @Autowired
    private OrderItemApi orderItemApi;

    @Autowired
    private OrderItemFlow orderItemFlow;

    public List<OrderItemData> getAll() throws ApiException {
        List<OrderItem> orderItems = orderItemApi.getAll();

        return orderItemFlow.createOrderItemsData(orderItems);
    }

    public List<OrderItemData> getByOrderId(Integer orderId) throws ApiException{
        List<OrderItem> orderItems = orderItemApi.getAllByOrderId(orderId);

        return orderItemFlow.createOrderItemsData(orderItems);
    }

    public OrderItemData updateById(Integer orderId, Integer itemId, OrderItemUpdateForm orderItemUpdateForm) throws ApiException {
        OrderItem orderItem = OrderItemUtil.convert(orderItemUpdateForm,orderId,itemId);

        OrderItem updatedItem = orderItemFlow.update(orderItem);

        return orderItemFlow.createOrderItemData(updatedItem);
    }

    public void deleteById(Integer orderId, Integer itemId) throws ApiException {
        orderItemFlow.deleteById(orderId,itemId);
    }

    public OrderItemData add(Integer orderId, OrderItemForm orderItemForm) throws ApiException{
        OrderItem orderItem = OrderItemUtil.convert(orderItemForm,orderId);

        OrderItem insertedItem = orderItemFlow.add(orderItem);

        return orderItemFlow.createOrderItemData(insertedItem);
    }
}
