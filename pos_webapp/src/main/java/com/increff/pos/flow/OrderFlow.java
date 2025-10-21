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
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.data.PaginationData;
import com.increff.pos.model.enums.OrderStatus;
import com.increff.pos.model.result.OrderResult;
import com.increff.pos.model.result.PaginatedOrderResult;
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
        //Insert order
        order.setTotalAmount(OrderItemUtil.calculateTotalAmount(orderItems));
        orderApi.insert(order);
        OrderItemUtil.setOrderId(orderItems,order.getId());

        //Insert orderItems
        insertOrderItems(orderItems);

        //Return orderItemsData
        OrderResult orderResult = new OrderResult();
        orderResult.setOrder(order);
        orderResult.setOrderItems(orderItems);
        return orderResult;
    }

    public List<OrderResult> getAll() throws ApiException {
        // --- Step 1: Fetch all parent 'Order' entities in one query ---
        List<Order> allOrders = orderApi.getAll();

        if (allOrders.isEmpty()) {
            return new ArrayList<>(); // Return an empty list if there are no orders
        }

        // --- Step 2: High-Performance Bulk Fetch for all children 'OrderItems' ---
        // a. Extract all order IDs.
        List<Integer> orderIds = allOrders.stream()
                .map(Order::getId)
                .collect(Collectors.toList());

        // b. Fetch all related OrderItems in a SINGLE second query.
        List<OrderItem> allItems = orderItemApi.getByOrderIds(orderIds);

        // c. Group the items by their parent orderId for easy and fast lookup.
        Map<Integer, List<OrderItem>> itemsByOrderIdMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        // --- Step 3: Assemble the Final List<OrderResult> ---
        // Loop through each order and create a complete OrderResult object for it.
        return allOrders.stream()
                .map(order -> {
                    OrderResult result = new OrderResult();
                    result.setOrder(order);
                    // Get the list of items for this specific order from the map.
                    List<OrderItem> items = itemsByOrderIdMap.getOrDefault(order.getId(), new ArrayList<>());
                    result.setOrderItems(items);
                    return result;
                })
                .collect(Collectors.toList());
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

    public PaginatedOrderResult getByFilters(ZonedDateTime startDate, ZonedDateTime endDate, OrderStatus status, Pageable pageable) throws ApiException {
        // --- Step 1: Fetch data and count from the API layer ---
        List<Order> ordersOnPage = orderApi.getByFilters(startDate, endDate, status, pageable);
        Long totalElements = orderApi.countWithFilters(startDate, endDate, status);

        // --- Step 2: Handle the case where no orders are found ---
        if (ordersOnPage.isEmpty()) {
            PaginatedOrderResult emptyResult = new PaginatedOrderResult();
            emptyResult.setOrderResults(new ArrayList<>());
            emptyResult.setTotalElements(0L);
            emptyResult.setTotalPages(0);
            return emptyResult;
        }

        // --- Step 3: High-Performance Bulk Fetch for Children 'OrderItems' ---
        List<Integer> orderIds = ordersOnPage.stream().map(Order::getId).collect(Collectors.toList());
        List<OrderItem> allItemsForPage = orderItemApi.getByOrderIds(orderIds);
        Map<Integer, List<OrderItem>> itemsByOrderIdMap = allItemsForPage.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        // --- Step 4: Assemble the List<OrderResult> here in the Flow layer ---
        List<OrderResult> orderResults = ordersOnPage.stream()
                .map(order -> {
                    OrderResult orderResult = new OrderResult();
                    orderResult.setOrder(order);
                    List<OrderItem> items = itemsByOrderIdMap.getOrDefault(order.getId(), new ArrayList<>());
                    orderResult.setOrderItems(items);
                    return orderResult;
                })
                .collect(Collectors.toList());

        // --- Step 5: Construct the final result container ---
        PaginatedOrderResult finalResult = new PaginatedOrderResult();
        finalResult.setOrderResults(orderResults);
        finalResult.setTotalElements(totalElements);

        // IMPROVEMENT: Manually calculate total pages to remove the need for a temporary Page object.
        int pageSize = pageable.getPageSize();
        int totalPages = (pageSize == 0) ? 1 : (int) Math.ceil((double) totalElements / (double) pageSize);
        finalResult.setTotalPages(totalPages);

        return finalResult;
    }

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

    public PaginationData<OrderData> convert(PaginatedOrderResult result) throws ApiException{
        // --- Step 1: High-Performance Bulk Fetch for all needed Products ---
        List<Integer> productIds = result.getOrderResults().stream()
                .flatMap(res -> res.getOrderItems().stream())
                .map(OrderItem::getProductId)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, Product> productMap = productApi.getByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // --- Step 2: Convert each OrderResult into an OrderData object ---
        List<OrderData> orderDataList = result.getOrderResults().stream()
                .map(orderResult -> OrderUtil.convert(orderResult.getOrder(), orderResult.getOrderItems(), productMap))
                .collect(Collectors.toList());

        // --- Step 3: Construct the final PaginationData object ---
        PaginationData<OrderData> paginationData = new PaginationData<>();
        paginationData.setContent(orderDataList);
        paginationData.setTotalPages(result.getTotalPages());
        paginationData.setTotalElements(result.getTotalElements());

        return paginationData;
    }

    public OrderData convert(OrderResult orderResult) throws ApiException {
        // --- Step 1: High-Performance Bulk Fetch for all needed Products ---
        // a. Extract all unique product IDs from the order items.
        List<Integer> productIds = orderResult.getOrderItems().stream()
                .map(OrderItem::getProductId)
                .distinct()
                .collect(Collectors.toList());

        // b. Fetch all required Product entities in a SINGLE query.
        Map<Integer, Product> productMap = productApi.getByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // --- Step 2: Delegate to the Utility for Final Conversion ---
        // Pass all the necessary data (Order, OrderItems, and the new productMap) to the utility.
        return OrderUtil.convert(orderResult.getOrder(), orderResult.getOrderItems(), productMap);
    }
}
