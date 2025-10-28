package com.increff.pos.dao;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import org.springframework.data.domain.Pageable;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


//Generic Functions for All Dao Classes

@Repository
public abstract class  AbstractDao<T> {

    private final Class<T> clazz;

    @SuppressWarnings("unchecked")
    public AbstractDao() {
        this.clazz = (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass())
                .getActualTypeArguments()[0];
    }

    @PersistenceContext
    protected EntityManager em;

    public void insert(T pojo){
        em.persist(pojo);
    }

    @Transactional
    public void insertAll(List<T> pojos) {
        // Define the size of each batch for optimal performance.
        final int batchSize = 50;
        int i = 0;

        for (T pojo : pojos) {
            // Persist the entity. At this point, it's managed by the persistence context
            // but not yet sent to the database as an individual INSERT statement.
            em.persist(pojo);
            i++;

            // When the batch size is reached, flush the changes to the database
            // and clear the context to free up memory.
            if (i % batchSize == 0) {
                em.flush(); // Send the batched INSERT statements to the DB
                em.clear(); // Detach all managed entities from the context to save memory
            }
        }
        // A final flush may be needed if the last batch is smaller than batchSize.
        // However, the transaction commit at the end of the method will handle this automatically.
    }

    public T selectById(Integer id){
        return em.find(clazz,id);
    }

    public List<T> selectByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        String selectQuery = "select p from " + clazz.getName() + " p where id in :ids";
        TypedQuery<T> query = em.createQuery(selectQuery, clazz);
        query.setParameter("ids", ids);
        return query.getResultList();
    }

    public List<T> selectAll(){
        String selectQuery = "select p from " + clazz.getName()+" p";
        TypedQuery<T> query = em.createQuery(selectQuery,clazz);
        return query.getResultList();
    }

    public void deleteById(Integer id){
        T obj = selectById(id);
        em.remove(obj);
    }

    public void update(T obj){
        em.merge(obj);
    }

    protected TypedQuery<T> getQuery(String query){
        return em.createQuery(query,clazz);
    }

    protected T getFirstRowFromQuery(TypedQuery<T> query) {
        try {
            return query.setMaxResults(1).getSingleResult();
        } catch (NoResultException e) {
            return null; // Return null if no result found
        }
    }

    protected <R> TypedQuery<R> buildQuery(String jpql, Class<R> resultClazz, Map<String, Object> params) {
        TypedQuery<R> query = em.createQuery(jpql, resultClazz);
        setParameters(query, params);
        return query;
    }

    protected  List<T> getResultList(String jpql, Map<String, Object> params) {
        TypedQuery<T> query = buildQuery(jpql, clazz, params);
        return query.getResultList();
    }

    /**
     * Convenience method to execute a query for a DTO/Result type
     * and return the list of results.
     */
    protected <R> List<R> getCustomResultList(String jpql, Class<R> resultClazz, Map<String, Object> params) {
        TypedQuery<R> query = buildQuery(jpql, resultClazz, params);
        return query.getResultList();
    }

    /**
     * Executes a pre-built CriteriaQuery that returns a list of results (of type R).
     * Handles pagination.
     * @param criteriaQuery The CriteriaQuery object built by the concrete DAO.
     * @param pageable      The Pageable object for pagination/sorting (can be null or unpaged).
     * @param <R>           The type of the result list elements.
     * @return A list of results.
     */
    protected <R> List<R> executeCriteriaQueryList(CriteriaQuery<R> criteriaQuery, Pageable pageable) {
        TypedQuery<R> query = em.createQuery(criteriaQuery);

        // Apply pagination if specified
        if (pageable != null && pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }

        return query.getResultList();
    }

    /**
     * Executes a pre-built CriteriaQuery that returns a single result (e.g., a COUNT).
     * @param criteriaQuery The CriteriaQuery object built by the concrete DAO.
     * @param <R>           The type of the single result.
     * @return The single result.
     */
    protected <R> R executeCriteriaQuerySingleResult(CriteriaQuery<R> criteriaQuery) {
        TypedQuery<R> query = em.createQuery(criteriaQuery);
        return query.getSingleResult();
    }

    protected CriteriaBuilder getCriteriaBuilder() {
        return this.em.getCriteriaBuilder();
    }

    private <Q> void setParameters(TypedQuery<Q> query, Map<String, Object> params) {
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }
        }
    }
}

