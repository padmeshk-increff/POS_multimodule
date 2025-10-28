package com.increff.pos.controller;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dto.OrderDto;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.data.PaginationData;
import com.increff.pos.model.enums.OrderStatus;
import com.increff.pos.model.form.OrderForm;
import com.increff.pos.model.form.OrderUpdateForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderDto orderDto;

    @RequestMapping(method= RequestMethod.POST)
    public OrderData add(@RequestBody OrderForm orderForm) throws ApiException {
        return orderDto.add(orderForm);
    }

    @RequestMapping(method = RequestMethod.GET)
    public PaginationData<OrderData> getFilteredOrders(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) throws ApiException {
        return orderDto.getFilteredOrders(id, startDate, endDate, status, page, size);
    }

    @RequestMapping(value="/{orderId}",method = RequestMethod.GET)
    public OrderData getById(@PathVariable(value="orderId")Integer orderId) throws ApiException{
        return orderDto.getById(orderId);
    }

    @RequestMapping(value="/{orderId}",method = RequestMethod.PUT)
    public OrderData updateById(@PathVariable(value="orderId")Integer orderId, @RequestBody OrderUpdateForm orderUpdateForm) throws ApiException{
        return orderDto.updateById(orderId,orderUpdateForm);
    }
}
