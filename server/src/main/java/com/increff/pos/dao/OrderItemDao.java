package com.increff.pos.dao;

import com.increff.pos.entity.OrderItem;
import com.increff.pos.model.enums.OrderStatus;
import com.increff.pos.model.result.ProductQuantityResult;
import com.increff.pos.model.result.SalesOverTimeResult;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class OrderItemDao extends AbstractDao<OrderItem> {

    private static final String SELECT_BY_ORDER_ID = "select p from OrderItem p where orderId = :orderId";
    private static final String SELECT_BY_ORDER_ID_AND_PRODUCT_ID  = "select p from OrderItem p where orderId = :orderId and productId = :productId";
    private static final String SELECT_BY_ORDER_IDS = "select p from OrderItem p where orderId in :orderIds";
    private static final String FIND_TOP_SELLING =
            "SELECT NEW com.increff.pos.model.result.ProductQuantityResult(oi.productId, SUM(oi.quantity), SUM(oi.quantity * oi.sellingPrice)) " +
                    "FROM OrderItem oi JOIN Order o ON oi.orderId = o.id " +
                    "WHERE o.createdAt >= :start AND o.createdAt < :end " +
                    "AND o.orderStatus = :status " +
                    "GROUP BY oi.productId " +
                    "ORDER BY SUM(oi.quantity) DESC";

    private static final String FIND_SALES_BY_DATE =
            "SELECT NEW com.increff.pos.model.result.SalesOverTimeResult( " +
                    "   DATE(o.createdAt), " + // Use DATE() for MySQL
                    "   SUM(oi.sellingPrice * oi.quantity) as totalRevenue " +
                    ") " +
                    "FROM OrderItem oi JOIN Order o ON oi.orderId = o.id " +
                    "WHERE o.createdAt >= :start AND o.createdAt < :end " +
                    "AND o.orderStatus = :status " +
                    "GROUP BY DATE(o.createdAt) " + // Group by DATE()
                    "ORDER BY DATE(o.createdAt) ASC";

    public List<ProductQuantityResult> findTopSellingProducts(ZonedDateTime start, ZonedDateTime end, Pageable pageable, OrderStatus status) {
        // Create parameter map
        Map<String, Object> params = new HashMap<>();
        params.put("start", start);
        params.put("end", end);
        params.put("status", status);

        // Build the query using the helper
        TypedQuery<ProductQuantityResult> query = buildQuery(
                FIND_TOP_SELLING,
                ProductQuantityResult.class,
                params
        );

        // Apply pagination from Pageable *after* building the query
        if (pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset()); // Still need offset for pagination
            query.setMaxResults(pageable.getPageSize());
        }

        // Execute the query
        return query.getResultList();
    }

    public List<SalesOverTimeResult> findSalesByDate(ZonedDateTime start, ZonedDateTime end, OrderStatus status) {
        // Create parameter map
        Map<String, Object> params = new HashMap<>();
        params.put("start", start);
        params.put("end", end);
        params.put("status", status);

        // Use the convenience method for custom result types
        return getCustomResultList(FIND_SALES_BY_DATE, SalesOverTimeResult.class, params);
    }

    public List<OrderItem> selectByOrderId(Integer orderId) {
        TypedQuery<OrderItem> query = getQuery(SELECT_BY_ORDER_ID);
        query.setParameter("orderId", orderId);
        return query.getResultList();
    }

    public OrderItem selectByOrderIdAndProductId(Integer orderId,Integer productId){
        TypedQuery<OrderItem> query = getQuery(SELECT_BY_ORDER_ID_AND_PRODUCT_ID);
        query.setParameter("orderId",orderId);
        query.setParameter("productId",productId);
        List<OrderItem> resultList = query.getResultList();
        return resultList.isEmpty() ? null : resultList.get(0);
    }

    public List<OrderItem> selectByOrderIds(List<Integer> orderIds) {
        TypedQuery<OrderItem> query = getQuery(SELECT_BY_ORDER_IDS);
        query.setParameter("orderIds", orderIds);
        return query.getResultList();
    }

}