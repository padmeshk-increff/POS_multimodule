package com.increff.pos.dao;

import com.increff.pos.entity.Invoice;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;

@Repository
public class InvoiceDao extends AbstractDao<Invoice> {

    private static final String SELECT_BY_ORDER_ID = "select i from Invoice i where i.orderId = :orderId";

    public Invoice selectByOrderId(Integer orderId) {
        TypedQuery<Invoice> query = getQuery(SELECT_BY_ORDER_ID);
        query.setParameter("orderId", orderId);
        return getFirstRowFromQuery(query);
    }
}
