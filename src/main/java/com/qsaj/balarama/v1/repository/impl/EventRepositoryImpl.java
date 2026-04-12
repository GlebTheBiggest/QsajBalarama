package com.qsaj.balarama.v1.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.qsaj.balarama.v1.model.Event;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.repository.EventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class EventRepositoryImpl implements EventRepository {

    private final Provider<EntityManager> emp;

    @Inject
    public EventRepositoryImpl(Provider<EntityManager> emp) {
        this.emp = emp;
    }

    @Override
    public void save(Event event) {
        EntityManager em = emp.get();
        EntityTransaction tx = em.getTransaction();
        boolean isNewTx = !tx.isActive();
        if (isNewTx) tx.begin();
        try {
            em.persist(event);
            if (isNewTx) tx.commit();
        } catch (Exception e) {
            if (isNewTx && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    @Override
    public List<Event> findAllByUser(User user) {
        EntityManager em = emp.get();
        TypedQuery<Event> query = em.createQuery(
                "SELECT e FROM Event e WHERE e.user = :user ORDER BY e.timestamp DESC", Event.class);
        query.setParameter("user", user);
        return query.getResultList();
    }

    @Override
    public List<Event> findRecentByUser(User user, int limit) {
        EntityManager em = emp.get();
        TypedQuery<Event> query = em.createQuery(
                "SELECT e FROM Event e WHERE e.user = :user ORDER BY e.timestamp DESC", Event.class);
        query.setParameter("user", user);
        query.setMaxResults(limit);
        return query.getResultList();
    }
}