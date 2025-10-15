package com.increff.pos.dao;

import com.increff.pos.entity.Inventory;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;

@Repository
public class InventoryDao extends AbstractDao<Inventory> {
    private static final String SELECT_BY_PRODUCT_ID = "select p from Inventory p where productId = :productId";

    public Inventory selectByProductId(Integer productId) {
        TypedQuery<Inventory> query = getQuery(SELECT_BY_PRODUCT_ID);
        query.setParameter("productId", productId);
        return getFirstRowFromQuery(query);
    }
}