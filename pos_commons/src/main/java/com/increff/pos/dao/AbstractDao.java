package com.increff.pos.dao;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;


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

    protected T getFirstRowFromQuery(TypedQuery<T> query){
        return query.getResultList().stream().findFirst().orElse(null);
    }
}

