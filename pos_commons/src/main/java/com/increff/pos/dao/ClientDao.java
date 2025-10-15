package com.increff.pos.dao;

import com.increff.pos.entity.Client;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import java.util.List;

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
}
