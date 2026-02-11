package com.increff.pos.dao;

import com.increff.pos.entity.Client;
import com.increff.pos.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
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

    public List<Product> selectWithFilters(String searchTerm, String clientName, String category, Double minMrp, Double maxMrp, Pageable pageable) {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> productRoot = cq.from(Product.class);

        Root<Client> clientRoot = cq.from(Client.class);

        cq.select(productRoot);

        List<Predicate> predicates = buildPredicates(cb, productRoot, clientRoot, searchTerm, clientName, category, minMrp, maxMrp);

        predicates.add(cb.equal(productRoot.get("clientId"), clientRoot.get("id")));

        cq.where(predicates.toArray(new Predicate[0]));

        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                if (order.isAscending()) {
                    cq.orderBy(cb.asc(productRoot.get(order.getProperty())));
                } else {
                    cq.orderBy(cb.desc(productRoot.get(order.getProperty())));
                }
            });
        } else {
            cq.orderBy(cb.asc(productRoot.get("id")));
        }

        return executeCriteriaQueryList(cq, pageable);
    }

    public Long countWithFilters(String searchTerm, String clientName, String category, Double minMrp, Double maxMrp) {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Product> productRoot = cq.from(Product.class);

        Root<Client> clientRoot = cq.from(Client.class);

        List<Predicate> predicates = buildPredicates(cb, productRoot, clientRoot, searchTerm, clientName, category, minMrp, maxMrp);

        predicates.add(cb.equal(productRoot.get("clientId"), clientRoot.get("id")));

        cq.where(predicates.toArray(new Predicate[0]));
        cq.select(cb.count(productRoot));

        return executeCriteriaQuerySingleResult(cq);
    }

    /**
     * Centralized helper method to build the list of filter predicates.
     */
    private List<Predicate> buildPredicates(
            CriteriaBuilder cb, Root<Product> productRoot, Root<Client> clientRoot,
            String searchTerm, String clientName, String category, Double minMrp, Double maxMrp
    ) {
        List<Predicate> predicates = new ArrayList<>();
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String searchTermLower = searchTerm.trim().toLowerCase();
            String prefixPattern = searchTermLower + "%";
            
            predicates.add(cb.or(
                    cb.like(productRoot.get("name"), prefixPattern),
                    cb.like(cb.lower(productRoot.get("barcode")), prefixPattern)
            ));
        }

        // Specific clientName filter (clientName is stored lowercase, so no LOWER() needed - uses index)
        if (clientName != null && !clientName.trim().isEmpty()) {
            predicates.add(cb.equal(clientRoot.get("clientName"), clientName.trim().toLowerCase())); // Use clientRoot
        }

        // Specific category filter (category is stored lowercase, so no LOWER() needed - uses idx_product_category)
        if (category != null && !category.trim().isEmpty()) {
            predicates.add(cb.equal(productRoot.get("category"), category.trim().toLowerCase()));
        }

        // MRP range filters
        if (minMrp != null) {
            predicates.add(cb.greaterThanOrEqualTo(productRoot.get("mrp"), minMrp));
        }
        if (maxMrp != null) {
            predicates.add(cb.lessThanOrEqualTo(productRoot.get("mrp"), maxMrp));
        }

        return predicates;
    }
}