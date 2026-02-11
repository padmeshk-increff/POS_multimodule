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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private void insertOrderItems(List<OrderItem> orderItems) throws ApiException {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new ApiException("Order must contain at least one item");
        }

        // Step 1: Collect all unique product IDs
        List<Integer> productIds = orderItems.stream()
                .map(OrderItem::getProductId)
                .distinct()
                .collect(Collectors.toList());

        // Step 2: Bulk fetch all required inventories and products (2 queries instead of 2N)
        // getCheckByProductIds() throws ApiException if any productId is missing inventory
        List<Inventory> inventories = inventoryApi.getCheckByProductIds(productIds);
        Map<Integer, Inventory> inventoryMap = inventories.stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        // getCheckByIds() throws ApiException if any productId is missing product
        List<Product> products = productApi.getCheckByIds(productIds);
        Map<Integer, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // Step 3: Validate all items and prepare inventory updates
        List<Inventory> inventoriesToUpdate = new ArrayList<>();
        
        for (OrderItem orderItem : orderItems) {
            Integer productId = orderItem.getProductId();
            
            // Inventory and product existence are already validated by getCheckByProductIds() and getCheckByIds()
            Inventory itemInventory = inventoryMap.get(productId);
            Product product = productMap.get(productId);

            // Validate stock availability
            Integer remainingQuantity = itemInventory.getQuantity() - orderItem.getQuantity();
            if (remainingQuantity < 0) {
                throw new ApiException("Not enough stock is available for product " + product.getName());
            }

            // Validate selling price
            if (orderItem.getSellingPrice() > product.getMrp()) {
                throw new ApiException("Selling price cannot be more than mrp for product " + product.getName());
            }

            // Prepare inventory update
            Inventory updatedInventory = new Inventory();
            updatedInventory.setProductId(productId);
            updatedInventory.setQuantity(remainingQuantity);
            inventoriesToUpdate.add(updatedInventory);
        }

        // Step 4: Bulk update inventories (1 optimized operation)
        inventoryApi.bulkUpdateInventories(inventoriesToUpdate);

        // Step 5: Bulk insert order items (1 batched operation)
        orderItemApi.insertAll(orderItems);
    }
}
