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
        EntityTransaction tx = null;
        try (EntityManager em = emp.get()) {
            tx = em.getTransaction();
            tx.begin();
            em.persist(budget);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    @Override
    public void update(CategoryBudget budget) {
        EntityTransaction tx = null;
        try (EntityManager em = emp.get()) {
            tx = em.getTransaction();
            tx.begin();
            em.merge(budget);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    @Override
    public List<CategoryBudget> findByPeriod(BudgetPeriod period) {
        try (EntityManager em = emp.get()) {
            return em.createQuery(
                            "SELECT b FROM CategoryBudget b WHERE b.budgetPeriod = :period", CategoryBudget.class)
                    .setParameter("period", period)
                    .getResultList();
        }
    }

    @Override
    public Optional<CategoryBudget> findByPeriodAndGroup(BudgetPeriod period, CategoryGroup group) {
        try (EntityManager em = emp.get()) {
            List<CategoryBudget> results = em.createQuery(
                            "SELECT b FROM CategoryBudget b WHERE b.budgetPeriod = :period AND b.categoryGroup = :group", CategoryBudget.class)
                    .setParameter("period", period)
                    .setParameter("group", group)
                    .getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        }
    }
}