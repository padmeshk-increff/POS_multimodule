package com.increff.pos.dao;

import com.increff.pos.entity.OrderItem;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import java.util.List;

@Repository
public class OrderItemDao extends AbstractDao<OrderItem> {

    private static final String SELECT_BY_ORDER_ID = "select p from OrderItem p where orderId = :orderId";
    private static final String SELECT_BY_ORDER_ID_AND_PRODUCT_ID  = "select p from OrderItem p where orderId = :orderId and productId = :productId";
    private static final String SELECT_BY_ORDER_IDS = "select p from OrderItem p where orderId in :orderIds";

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