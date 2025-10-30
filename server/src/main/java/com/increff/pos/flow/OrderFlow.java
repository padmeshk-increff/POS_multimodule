package com.increff.pos.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.OrderItemApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.model.enums.OrderStatus;
import com.increff.pos.model.result.OrderResult;
import com.increff.pos.model.result.PaginatedResult;
import com.increff.pos.utils.OrderItemUtil;
import com.increff.pos.utils.OrderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Component
@Transactional(rollbackFor = ApiException.class)
public class OrderFlow {

    @Autowired
    private OrderApi orderApi;

    @Autowired
    private ProductApi productApi;

    @Autowired
    private InventoryApi inventoryApi;

    @Autowired
    private OrderItemApi orderItemApi;

    public OrderResult insert(Order order, List<OrderItem> orderItems) throws ApiException{
        order.setTotalAmount(OrderItemUtil.calculateTotalAmount(orderItems));
        orderApi.insert(order);
        OrderItemUtil.setOrderId(orderItems,order.getId());

        insertOrderItems(orderItems);

        OrderResult orderResult = new OrderResult();
        orderResult.setOrder(order);
        orderResult.setOrderItems(orderItems);
        return orderResult;
    }

    public OrderResult getById(Integer id) throws ApiException{
        Order order = orderApi.getCheckById(id);
        List<OrderItem> orderItems = orderItemApi.getAllByOrderId(id);

        OrderResult orderResult = new OrderResult();
        orderResult.setOrder(order);
        orderResult.setOrderItems(orderItems);
        return orderResult;
    }

    public OrderResult updateById(Integer id,Order order)throws ApiException{
        Order updatedOrder = orderApi.updateById(id,order);
        List<OrderItem> orderItems = orderItemApi.getAllByOrderId(id);

        if(updatedOrder.getOrderStatus() == OrderStatus.CANCELLED){
            for(OrderItem orderItem:orderItems){
                inventoryApi.updateQuantityByProductId(orderItem.getProductId(),orderItem.getQuantity(),0);
            }
        }

        OrderResult orderResult = new OrderResult();
        orderResult.setOrder(updatedOrder);
        orderResult.setOrderItems(orderItems);
        return orderResult;
    }

    public PaginatedResult<OrderResult> getByFilters(Integer id, ZonedDateTime startDate, ZonedDateTime endDate, OrderStatus status, Pageable pageable) throws ApiException {
        List<Order> ordersOnPage = orderApi.getByFilters(id, startDate, endDate, status, pageable);
        Long totalElements = orderApi.countWithFilters(id, startDate, endDate, status);

        if (ordersOnPage.isEmpty()) {
            return OrderUtil.createEmptyResult();
        }

        List<Integer> orderIds = OrderUtil.getOrderIds(ordersOnPage);
        List<OrderItem> allItemsForPage = orderItemApi.getByOrderIds(orderIds);
        Map<Integer, List<OrderItem>> itemsByOrderIdMap = OrderItemUtil.mapItemsByOrderId(allItemsForPage);

        List<OrderResult> orderResults = OrderUtil.createOrderResults(ordersOnPage,itemsByOrderIdMap);

        PaginatedResult<OrderResult> finalResult = new PaginatedResult<>();
        finalResult.setResults(orderResults);
        finalResult.setTotalElements(totalElements);

        int pageSize = pageable.getPageSize();
        int totalPages = (pageSize == 0) ? 1 : (int) Math.ceil((double) totalElements / (double) pageSize);
        finalResult.setTotalPages(totalPages);

        return finalResult;
    }

    //todo: check if bulk insert is any different this for loop insert
    private void insertOrderItems(List<OrderItem> orderItems) throws ApiException {
        for(OrderItem orderItem:orderItems){
            Inventory itemInventory = inventoryApi.getCheckByProductId(orderItem.getProductId());
            Integer remainingQuantity = itemInventory.getQuantity() - orderItem.getQuantity();

            if(remainingQuantity<0){
                throw new ApiException("Not enough stock is available for product with id "+orderItem.getProductId());
            }

            itemInventory.setQuantity(remainingQuantity);
            inventoryApi.update(itemInventory);
            Product product = productApi.getCheckById(orderItem.getProductId());

            if(orderItem.getSellingPrice() > product.getMrp()){
                throw new ApiException("Selling price cannot be more than mrp");
            }

            orderItemApi.insert(orderItem);
        }
    }
}
