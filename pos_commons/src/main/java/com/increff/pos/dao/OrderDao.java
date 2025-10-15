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

    public List<Order> findWithFilters(ZonedDateTime startDate, ZonedDateTime endDate, OrderStatus status, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> orderRoot = cq.from(Order.class);

        List<Predicate> predicates = buildPredicates(cb, orderRoot, startDate, endDate, status);
        cq.where(predicates.toArray(new Predicate[0]));

        if (pageable.getSort().isSorted()) {
            cq.orderBy(cb.desc(orderRoot.get("id")));
        }

        TypedQuery<Order> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        return query.getResultList();
    }

    public Long countWithFilters(ZonedDateTime startDate, ZonedDateTime endDate, OrderStatus status) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Order> orderRoot = cq.from(Order.class);

        List<Predicate> predicates = buildPredicates(cb, orderRoot, startDate, endDate, status);
        cq.where(predicates.toArray(new Predicate[0]));

        cq.select(cb.count(orderRoot));

        return em.createQuery(cq).getSingleResult();
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Order> root, ZonedDateTime startDate, ZonedDateTime endDate, OrderStatus status) {
        List<Predicate> predicates = new ArrayList<>();
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        }
        if (status != null) {
            predicates.add(cb.equal(root.get("orderStatus"), status));
        }
        return predicates;
    }
}