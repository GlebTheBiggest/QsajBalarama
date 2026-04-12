package com.qsaj.balarama.v1.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.qsaj.balarama.v1.model.BudgetPeriod;
import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class TransactionRepositoryImpl implements TransactionRepository {

    private final Provider<EntityManager> emp;

    @Inject
    public TransactionRepositoryImpl(Provider<EntityManager> emp) {
        this.emp = emp;
    }

    @Override
    public void save(Transaction transaction) {
        EntityManager em = emp.get();
        EntityTransaction tx = em.getTransaction();
        boolean isNewTx = !tx.isActive();
        if (isNewTx) tx.begin();
        try {
            em.persist(transaction);
            if (isNewTx) tx.commit();
        } catch (Exception e) {
            if (isNewTx && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    @Override
    public List<Transaction> findAllByUser(User user) {
        EntityManager em = emp.get();
        TypedQuery<Transaction> query = em.createQuery(
                "SELECT t FROM Transaction t WHERE t.user = :user ORDER BY t.date DESC", Transaction.class);
        query.setParameter("user", user);
        return query.getResultList();
    }

    @Override
    public List<Transaction> findAllByBudgetPeriod(BudgetPeriod period) {
        EntityManager em = emp.get();
        TypedQuery<Transaction> query = em.createQuery(
                "SELECT t FROM Transaction t WHERE t.budgetPeriod = :period ORDER BY t.date DESC", Transaction.class);
        query.setParameter("period", period);
        return query.getResultList();
    }

    @Override
    public Transaction findById(Long id) {
        EntityManager em = emp.get();
        return em.find(Transaction.class, id);
    }

    @Override
    public void delete(Long id) {
        EntityManager em = emp.get();
        EntityTransaction tx = em.getTransaction();
        boolean isNewTx = !tx.isActive();
        if (isNewTx) tx.begin();
        try {
            Transaction t = em.find(Transaction.class, id);
            if (t != null) em.remove(t);
            if (isNewTx) tx.commit();
        } catch (Exception e) {
            if (isNewTx && tx.isActive()) tx.rollback();
            throw e;
        }
    }
}