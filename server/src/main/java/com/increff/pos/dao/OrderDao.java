package com.increff.pos.dao;

import com.increff.pos.entity.Order;
import com.increff.pos.model.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderDao extends AbstractDao<Order> {

    // Uses idx_order_created_at and idx_order_status indexes for efficient range queries
    private static final String SELECT_BY_DATE_RANGE =
            "SELECT o FROM Order o WHERE o.createdAt >= :start AND o.createdAt < :end " +
                    "AND o.orderStatus = :invoicedStatus";

    public List<Order> selectAllByDateRange(ZonedDateTime start, ZonedDateTime end) {
        TypedQuery<Order> query = getQuery(SELECT_BY_DATE_RANGE);
        query.setParameter("start", start);
        query.setParameter("end", end);
        query.setParameter("invoicedStatus", OrderStatus.INVOICED);
        return query.getResultList();
    }

    public List<Order> findWithFilters(Integer id, ZonedDateTime startDate, ZonedDateTime endDate, OrderStatus status, Pageable pageable) {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> orderRoot = cq.from(Order.class);

        List<Predicate> predicates = buildPredicates(cb, orderRoot, id, startDate, endDate, status);
        cq.where(predicates.toArray(new Predicate[0]));

        // Apply sorting
        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                if (order.isAscending()) {
                    cq.orderBy(cb.asc(orderRoot.get(order.getProperty())));
                } else {
                    cq.orderBy(cb.desc(orderRoot.get(order.getProperty())));
                }
            });
        } else {
            cq.orderBy(cb.desc(orderRoot.get("id"))); // Default sort by ID desc
        }

        // Delegate execution and pagination to AbstractDao helper
        return executeCriteriaQueryList(cq, pageable);
    }

    public Long countWithFilters(Integer id, ZonedDateTime startDate, ZonedDateTime endDate, OrderStatus status) {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Order> orderRoot = cq.from(Order.class);

        // Pass the id to buildPredicates
        List<Predicate> predicates = buildPredicates(cb, orderRoot, id, startDate, endDate, status);
        cq.where(predicates.toArray(new Predicate[0]));

        cq.select(cb.count(orderRoot));

        // Delegate execution to AbstractDao helper
        return executeCriteriaQuerySingleResult(cq);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Order> root, Integer id, ZonedDateTime startDate, ZonedDateTime endDate, OrderStatus status) {
        List<Predicate> predicates = new ArrayList<>();

        // ID filter - uses primary key index (automatic)
        if (id != null) {
            predicates.add(cb.equal(root.get("id"), id));
        }

        // Date range filters - uses idx_order_created_at index for efficient range queries
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        }

        // Status filter - uses idx_order_status index for exact match
        if (status != null) {
            predicates.add(cb.equal(root.get("orderStatus"), status));
        }

        return predicates;
    }
}