package com.increff.pos.dto;

import com.increff.pos.api.OrderApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.flow.OrderFlow;
import com.increff.pos.helper.OrderItemMapper;
import com.increff.pos.helper.OrderMapper;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.data.PaginationData;
import com.increff.pos.model.enums.OrderStatus;
import com.increff.pos.model.form.OrderForm;
import com.increff.pos.model.form.OrderUpdateForm;
import com.increff.pos.model.result.OrderResult;
import com.increff.pos.model.result.PaginatedResult;
import com.increff.pos.utils.OrderUtil;
import com.increff.pos.utils.ProductUtil;
import com.increff.pos.utils.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class OrderDto extends AbstractDto{

    @Autowired
    private OrderApi orderapi;

    @Autowired
    private ProductApi productApi;

    @Autowired
    private OrderFlow orderFlow;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    public OrderData add(OrderForm orderForm) throws ApiException{
        ValidationUtil.validate(orderForm);
        normalize(orderForm, Arrays.asList("customerPhone"));

        Order order = orderMapper.convert(orderForm);
        List<OrderItem> orderItems = orderItemMapper.convert(orderForm.getItems());
        OrderResult orderResult = orderFlow.insert(order,orderItems);

        List<Integer> productIds = OrderUtil.getProductIds(orderResult);
        List<Product> products = productApi.getByIds(productIds);
        Map<Integer, Product> productMap = ProductUtil.mapById(products);

        return orderMapper.convert(orderResult, productMap);
    }

    public OrderData updateById(Integer orderId, OrderUpdateForm orderUpdateForm) throws ApiException{
        ValidationUtil.validate(orderUpdateForm);
        normalize(orderUpdateForm, Arrays.asList("customerPhone"));

        Order order = orderMapper.convert(orderUpdateForm);

        OrderResult orderResult = orderFlow.updateById(orderId,order);

        List<Integer> productIds = OrderUtil.getProductIds(orderResult);
        List<Product> products = productApi.getByIds(productIds);
        Map<Integer, Product> productMap = ProductUtil.mapById(products);

        return orderMapper.convert(orderResult,productMap);
    }

    public OrderData getById(Integer orderId) throws ApiException {
        OrderResult orderResult= orderFlow.getById(orderId);

        List<Integer> productIds = OrderUtil.getProductIds(orderResult);
        List<Product> products = productApi.getByIds(productIds);
        Map<Integer, Product> productMap = ProductUtil.mapById(products);

        return orderMapper.convert(orderResult, productMap);
    }

    public PaginationData<OrderData> getFilteredOrders(Integer orderId,ZonedDateTime startDate, ZonedDateTime endDate, OrderStatus status, int page, int size) throws ApiException{
        Pageable pageable = PageRequest.of(page,size, Sort.by("id").descending());

        PaginatedResult<OrderResult> paginatedResult = orderFlow.getByFilters(orderId,startDate,endDate,status,pageable);

        List<Integer> productIds = OrderUtil.getProductIds(paginatedResult);
        List<Product> products = productApi.getByIds(productIds);
        Map<Integer, Product> productMap = ProductUtil.mapById(products);

        return orderMapper.convert(paginatedResult,productMap);
    }
}
