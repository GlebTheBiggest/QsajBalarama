package com.qsaj.balarama.v1.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.qsaj.balarama.v1.model.BudgetPeriod;
import com.qsaj.balarama.v1.model.CategoryBudget;
import com.qsaj.balarama.v1.model.CategoryGroup;
import com.qsaj.balarama.v1.repository.CategoryBudgetRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.Optional;

public class CategoryBudgetRepositoryImpl implements CategoryBudgetRepository {

    private final Provider<EntityManager> emp;

    @Inject
    public CategoryBudgetRepositoryImpl(Provider<EntityManager> emp) {
        this.emp = emp;
    }

    @Override
    public void save(CategoryBudget budget) {
        EntityManager em = emp.get();
        EntityTransaction tx = em.getTransaction();
        boolean isNewTx = !tx.isActive();
        if (isNewTx) tx.begin();
        try {
            em.persist(budget);
            if (isNewTx) tx.commit();
        } catch (Exception e) {
            if (isNewTx && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    @Override
    public void update(CategoryBudget budget) {
        EntityManager em = emp.get();
        EntityTransaction tx = em.getTransaction();
        boolean isNewTx = !tx.isActive();
        if (isNewTx) tx.begin();
        try {
            em.merge(budget);
            if (isNewTx) tx.commit();
        } catch (Exception e) {
            if (isNewTx && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    @Override
    public void delete(CategoryBudget budget) {
        EntityManager em = emp.get();
        EntityTransaction tx = em.getTransaction();
        boolean isNewTx = !tx.isActive();
        if (isNewTx) tx.begin();
        try {
            em.remove(em.contains(budget) ? budget : em.merge(budget));
            if (isNewTx) tx.commit();
        } catch (Exception e) {
            if (isNewTx && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    @Override
    public List<CategoryBudget> findByPeriod(BudgetPeriod period) {
        EntityManager em = emp.get();
        return em.createQuery(
                        "SELECT b FROM CategoryBudget b WHERE b.budgetPeriod = :period", CategoryBudget.class)
                .setParameter("period", period)
                .getResultList();
    }

    @Override
    public Optional<CategoryBudget> findByPeriodAndGroup(BudgetPeriod period, CategoryGroup group) {
        EntityManager em = emp.get();
        List<CategoryBudget> results = em.createQuery(
                        "SELECT b FROM CategoryBudget b WHERE b.budgetPeriod = :period AND b.categoryGroup = :group", CategoryBudget.class)
                .setParameter("period", period)
                .setParameter("group", group)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}