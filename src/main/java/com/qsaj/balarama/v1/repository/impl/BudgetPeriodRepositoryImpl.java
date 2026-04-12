package com.qsaj.balarama.v1.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.qsaj.balarama.v1.model.BudgetPeriod;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.repository.BudgetPeriodRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.Optional;

public class BudgetPeriodRepositoryImpl implements BudgetPeriodRepository {

    private final Provider<EntityManager> emp;

    @Inject
    public BudgetPeriodRepositoryImpl(Provider<EntityManager> emp) {
        this.emp = emp;
    }

    @Override
    public void save(BudgetPeriod period) {
        EntityManager em = emp.get();
        EntityTransaction tx = em.getTransaction();
        boolean isNewTx = !tx.isActive();
        if (isNewTx) tx.begin();
        try {
            em.persist(period);
            if (isNewTx) tx.commit();
        } catch (Exception e) {
            if (isNewTx && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    @Override
    public void update(BudgetPeriod period) {
        EntityManager em = emp.get();
        EntityTransaction tx = em.getTransaction();
        boolean isNewTx = !tx.isActive();
        if (isNewTx) tx.begin();
        try {
            em.merge(period);
            if (isNewTx) tx.commit();
        } catch (Exception e) {
            if (isNewTx && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    @Override
    public Optional<BudgetPeriod> findActiveByUser(User user) {
        EntityManager em = emp.get();
        List<BudgetPeriod> results = em.createQuery(
                        "SELECT p FROM BudgetPeriod p WHERE p.user = :user AND p.active = true", BudgetPeriod.class)
                .setParameter("user", user)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<BudgetPeriod> findAllByUser(User user) {
        EntityManager em = emp.get();
        return em.createQuery(
                        "SELECT p FROM BudgetPeriod p WHERE p.user = :user ORDER BY p.startDate DESC", BudgetPeriod.class)
                .setParameter("user", user)
                .getResultList();
    }
}