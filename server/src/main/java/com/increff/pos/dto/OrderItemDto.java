package com.increff.pos.dto;

import com.increff.pos.api.OrderItemApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.flow.OrderItemFlow;
import com.increff.pos.helper.OrderItemMapper;
import com.increff.pos.model.data.OrderItemData;
import com.increff.pos.model.form.OrderItemForm;
import com.increff.pos.model.form.OrderItemUpdateForm;
import com.increff.pos.utils.ProductUtil;
import com.increff.pos.utils.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderItemDto extends AbstractDto{

    @Autowired
    private OrderItemApi orderItemApi;

    @Autowired
    private OrderItemFlow orderItemFlow;

    @Autowired
    private ProductApi productApi;

    @Autowired
    private OrderItemMapper orderItemMapper;

    public List<OrderItemData> getAll() throws ApiException {
        List<OrderItem> orderItems = orderItemApi.getAll();

        List<Integer> productIds = orderItems.stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toList());

        List<Product> products = productApi.getByIds(productIds);
        Map<Integer,Product> productsMap = ProductUtil.mapById(products);

        return orderItemMapper.convert(orderItems,productsMap);
    }

    public List<OrderItemData> getByOrderId(Integer orderId) throws ApiException{
        List<OrderItem> orderItems = orderItemApi.getAllByOrderId(orderId);

        List<Integer> productIds = orderItems.stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toList());

        List<Product> products = productApi.getByIds(productIds);
        Map<Integer,Product> productsMap = ProductUtil.mapById(products);
        return orderItemMapper.convert(orderItems,productsMap);
    }

    public OrderItemData updateById(Integer orderId, Integer itemId, OrderItemUpdateForm orderItemUpdateForm) throws ApiException {
        ValidationUtil.validate(orderItemUpdateForm);

        OrderItem orderItem = orderItemMapper.convert(orderItemUpdateForm,orderId,itemId);

        OrderItem updatedItem = orderItemFlow.update(orderItem);

        Product product = productApi.getCheckById(orderItem.getProductId());
        return orderItemMapper.convert(updatedItem,product);
    }

    public void deleteById(Integer orderId, Integer itemId) throws ApiException {
        orderItemFlow.deleteById(orderId,itemId);
    }

    public OrderItemData add(Integer orderId, OrderItemForm orderItemForm) throws ApiException{
        ValidationUtil.validate(orderItemForm);

        OrderItem orderItem = orderItemMapper.convert(orderItemForm,orderId);

        OrderItem insertedItem = orderItemFlow.add(orderItem);

        Product product = productApi.getCheckById(orderItem.getProductId());
        return orderItemMapper.convert(insertedItem,product);
    }
}
