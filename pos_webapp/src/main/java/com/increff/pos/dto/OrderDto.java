package com.increff.pos.dto;

import com.increff.pos.api.OrderApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.flow.OrderFlow;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.data.PaginationData;
import com.increff.pos.model.enums.OrderStatus;
import com.increff.pos.model.form.OrderForm;
import com.increff.pos.model.form.OrderUpdateForm;
import com.increff.pos.model.result.OrderResult;
import com.increff.pos.model.result.PaginatedOrderResult;
import com.increff.pos.utils.NormalizeUtil;
import com.increff.pos.utils.OrderItemUtil;
import com.increff.pos.utils.OrderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Component
public class OrderDto {

    @Autowired
    private OrderApi orderapi;

    @Autowired
    private OrderFlow orderFlow;

    public OrderData add(OrderForm orderForm) throws ApiException{
        NormalizeUtil.normalize(orderForm);

        Order order = OrderUtil.convert(orderForm);
        List<OrderItem> orderItems = OrderItemUtil.convert(orderForm.getItems());
        OrderResult orderResult = orderFlow.insert(order,orderItems);

        return orderFlow.convert(orderResult);
    }

    public OrderData updateById(Integer orderId, OrderUpdateForm orderUpdateForm) throws ApiException{
        Order order = OrderUtil.convert(orderUpdateForm);

        OrderResult orderResult = orderFlow.updateById(orderId,order);

        return orderFlow.convert(orderResult);
    }

    public OrderData getById(Integer orderId) throws ApiException {
        OrderResult orderResult= orderFlow.getById(orderId);

        return orderFlow.convert(orderResult);
    }

    public PaginationData<OrderData> getFilteredOrders(ZonedDateTime startDate, ZonedDateTime endDate, OrderStatus status, int page, int size) throws ApiException{
        Pageable pageable = PageRequest.of(page,size, Sort.by("id").descending());

        PaginatedOrderResult paginatedOrderResult = orderFlow.getByFilters(startDate,endDate,status,pageable);

        return orderFlow.convert(paginatedOrderResult);
    }
}
