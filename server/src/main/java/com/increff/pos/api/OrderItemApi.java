package com.increff.pos.api;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dao.OrderItemDao;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.model.enums.OrderStatus;
import com.increff.pos.model.result.ProductQuantityResult;
import com.increff.pos.model.result.SalesOverTimeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(rollbackFor = ApiException.class)
public class OrderItemApi extends AbstractApi{
    @Autowired
    private OrderItemDao orderItemDao;

    public List<OrderItem> getAll(){
        return orderItemDao.selectAll();
    }

    public List<OrderItem> getByOrderIds(List<Integer> orderIds) throws ApiException{
        checkNull(orderIds, "order Ids cannot be null");

        if (orderIds.isEmpty()) {
            return new ArrayList<>();
        }

        return orderItemDao.selectByOrderIds(orderIds);
    }

    public List<OrderItem> getAllByOrderId(Integer orderId) throws ApiException{
        checkNull(orderId,"Order id cannot be null");

        return orderItemDao.selectByOrderId(orderId);
    }

    public OrderItem getCheckById(Integer id) throws ApiException{
        checkNull(id,"Id cannot be null");

        OrderItem orderItem = orderItemDao.selectById(id);
        checkNull(orderItem,"Order Item doesn't exist");

        return orderItem;
    }

    public OrderItem insert(OrderItem orderItem) throws ApiException{
        checkNull(orderItem,"Order Item cannot be null");

        OrderItem existingOrderItem = orderItemDao.selectByOrderIdAndProductId(orderItem.getOrderId(), orderItem.getProductId());
        checkNotNull(existingOrderItem,"Order Item already exists");

        orderItemDao.insert(orderItem);
        return orderItem;
    }


    public List<ProductQuantityResult> getTopSellingProducts(ZonedDateTime start, ZonedDateTime end, Integer limit) throws ApiException {
        checkNull(start, "Start date cannot be null");
        checkNull(end, "End date cannot be null");

        if (start.isAfter(end)) {
            throw new ApiException("Start date cannot be after end date.");
        }

        Pageable pageable = (limit != null) ? PageRequest.of(0, limit) : Pageable.unpaged();
        return orderItemDao.findTopSellingProducts(start, end, pageable, OrderStatus.INVOICED);
    }

    public List<SalesOverTimeResult> getSalesByDate(ZonedDateTime start, ZonedDateTime end) throws ApiException {
        checkNull(start, "Start date cannot be null");
        checkNull(end, "End date cannot be null");

        if (start.isAfter(end)) {
            throw new ApiException("Start date cannot be after end date.");
        }

        return orderItemDao.findSalesByDate(start, end,OrderStatus.INVOICED);
    }

    public OrderItem update(OrderItem orderItem) throws ApiException{
        checkNull(orderItem,"Order item object cannot be null");

        OrderItem existingOrderItem = orderItemDao.selectById(orderItem.getId());
        checkNull(existingOrderItem,"Order item doesn't exist");

        if(!Objects.equals(existingOrderItem.getOrderId(), orderItem.getOrderId())){
            throw new ApiException("Order "+orderItem.getOrderId()+" doesn't have order item "+orderItem.getId());
        }

        existingOrderItem.setQuantity(orderItem.getQuantity());
        existingOrderItem.setSellingPrice(orderItem.getSellingPrice());

        orderItemDao.insert(existingOrderItem);
        return existingOrderItem;
    }

    public void deleteById(Integer id) throws ApiException{
        checkNull(id,"Id cannot be null");

        OrderItem existingOrderItem = orderItemDao.selectById(id);
        checkNull(existingOrderItem,"Order Item doesn't exist");

        orderItemDao.deleteById(id);
    }

    public void deleteById(Integer itemId,Integer orderId) throws ApiException{
        checkNull(itemId,"Item id cannot be null");
        checkNull(orderId,"Order Id cannot be null");

        OrderItem existingOrderItem = orderItemDao.selectById(itemId);
        checkNull(existingOrderItem,"Order item doesn't exist");

        if(!Objects.equals(existingOrderItem.getOrderId(), orderId)){
            throw new ApiException("Order "+orderId+" doesn't have the item "+itemId);
        }

        orderItemDao.deleteById(itemId);
    }
}
