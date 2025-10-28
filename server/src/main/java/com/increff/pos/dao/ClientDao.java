package com.increff.pos.dao;

import com.increff.pos.entity.Client;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.ArrayList;
import javax.persistence.criteria.Predicate;

@Repository
public class ClientDao extends AbstractDao<Client>{

    private static final String SELECT_BY_NAME = "select p from Client p where clientName=:clientName";
    private static final String SELECT_BY_NAMES = "select c from Client c where clientName in :names";

    public Client selectByName(String clientName) {
        TypedQuery<Client> query = getQuery(SELECT_BY_NAME);
        query.setParameter("clientName", clientName);

        return getFirstRowFromQuery(query);
    }

    public List<Client> selectByNames(List<String> names) {
        TypedQuery<Client> query = getQuery(SELECT_BY_NAMES);
        query.setParameter("names", names);
        return query.getResultList();
    }

    public List<Client> selectWithFilters(String clientName, Pageable pageable) {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Client> cq = cb.createQuery(Client.class);
        Root<Client> clientRoot = cq.from(Client.class);

        List<Predicate> predicates = buildPredicates(cb, clientRoot, clientName);
        cq.where(predicates.toArray(new Predicate[0]));
        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                if (order.isAscending()) {
                    cq.orderBy(cb.asc(clientRoot.get(order.getProperty())));
                } else {
                    cq.orderBy(cb.desc(clientRoot.get(order.getProperty())));
                }
            });
        } else {
            cq.orderBy(cb.asc(clientRoot.get("id")));
        }

        return executeCriteriaQueryList(cq, pageable);
    }

    public Long countWithFilters(String clientName) {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Client> clientRoot = cq.from(Client.class);

        List<Predicate> predicates = buildPredicates(cb, clientRoot, clientName);
        cq.where(predicates.toArray(new Predicate[0]));

        cq.select(cb.count(clientRoot));

        return executeCriteriaQuerySingleResult(cq);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Client> root, String clientName) {
        List<Predicate> predicates = new ArrayList<>();

        if (clientName != null && !clientName.trim().isEmpty()) {
            predicates.add(cb.like(
                    cb.lower(root.get("clientName")),
                    "%" + clientName.toLowerCase() + "%"
            ));
        }

        return predicates;
    }
}
