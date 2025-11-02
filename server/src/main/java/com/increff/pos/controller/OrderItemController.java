package com.increff.pos.controller;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dto.OrderItemDto;
import com.increff.pos.model.data.OrderItemData;
import com.increff.pos.model.form.OrderItemForm;
import com.increff.pos.model.form.OrderItemUpdateForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderItemController {

    @Autowired
    private OrderItemDto orderItemDto;

    @RequestMapping(value="/items",method = RequestMethod.GET)
    public List<OrderItemData> getAll() throws ApiException{
        return orderItemDto.getAll();
    }

    @RequestMapping(value="/{orderId}/items",method = RequestMethod.GET)
    public List<OrderItemData> getByOrderId(@PathVariable(value="orderId")Integer orderId) throws ApiException{
        return orderItemDto.getByOrderId(orderId);
    }

    @RequestMapping(value="/{orderId}/items/{itemId}",method = RequestMethod.PUT)
    public OrderItemData updateByOrderId(@PathVariable(value = "orderId")Integer orderId, @PathVariable(value="itemId")Integer itemId, @RequestBody OrderItemUpdateForm form) throws ApiException{
        return orderItemDto.updateById(orderId,itemId,form);
    }

    @RequestMapping(value="/{orderId}/items/{itemId}",method = RequestMethod.DELETE)
    public void deleteById(@PathVariable(value="orderId")Integer orderId,@PathVariable(value="itemId")Integer itemId) throws ApiException{
        orderItemDto.deleteById(orderId,itemId);
    }

    @RequestMapping(value="/{orderId}/items", method= RequestMethod.POST)
    public OrderItemData add(@PathVariable(value="orderId")Integer orderId,@RequestBody OrderItemForm form) throws ApiException{
        return orderItemDto.add(orderId,form);
    }
}
