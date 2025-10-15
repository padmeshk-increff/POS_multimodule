package com.increff.pos.dao;

import com.increff.pos.entity.Product;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import java.util.List;

@Repository
public class ProductDao extends AbstractDao<Product> {

    private static final String SELECT_BY_BARCODE = "select p from Product p where barcode = :barcode";
    private static final String SELECT_BY_BARCODES = "select p from Product p where barcode in :barcodes";

    public Product selectByBarcode(String barcode) {
        TypedQuery<Product> query = getQuery(SELECT_BY_BARCODE);
        query.setParameter("barcode", barcode);
        return getFirstRowFromQuery(query);
    }

    public List<Product> selectByBarcodes(List<String> barcodes) {
        TypedQuery<Product> query = getQuery(SELECT_BY_BARCODES);
        query.setParameter("barcodes", barcodes);
        return query.getResultList();
    }
}