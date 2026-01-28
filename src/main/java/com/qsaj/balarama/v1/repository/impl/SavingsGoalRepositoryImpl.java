package com.qsaj.balarama.v1.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.qsaj.balarama.v1.model.SavingsGoal;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.repository.SavingsGoalRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;

public class SavingsGoalRepositoryImpl implements SavingsGoalRepository {

    private final Provider<EntityManager> emp;

    @Inject
    public SavingsGoalRepositoryImpl(Provider<EntityManager> emp) { this.emp = emp; }

    @Override
    public void save(SavingsGoal goal) {
        EntityManager em = emp.get();
        EntityTransaction tx = null;
        try (em) {
            tx = em.getTransaction();
            tx.begin();
            em.persist(goal);
            tx.commit();
        } catch (Exception e) {
            assert tx != null;
            if (tx.isActive()) tx.rollback();
            throw e;
        }
    }

    @Override
    public void update(SavingsGoal goal) {
        EntityManager em = emp.get();
        EntityTransaction tx = null;
        try (em) {
            tx = em.getTransaction();
            tx.begin();
            em.merge(goal);
            tx.commit();
        } catch (Exception e) {
            assert tx != null;
            if (tx.isActive()) tx.rollback();
            throw e;
        }
    }

    @Override
    public List<SavingsGoal> findAllByUser(User user) {
        try (EntityManager em = emp.get()) {
            return em.createQuery("SELECT s FROM SavingsGoal s WHERE s.user = :user", SavingsGoal.class)
                    .setParameter("user", user)
                    .getResultList();
        }
    }

    @Override
    public void delete(Long id) {
        EntityManager em = emp.get();
        EntityTransaction tx = null;
        try (em) {
            tx = em.getTransaction();
            tx.begin();
            SavingsGoal s = em.find(SavingsGoal.class, id);
            if (s != null) em.remove(s);
            tx.commit();
        } catch (Exception e) {
            assert tx != null;
            if (tx.isActive()) tx.rollback();
            throw e;
        }
    }

    @Override
    public SavingsGoal findById(Long id) {
        try (EntityManager em = emp.get()) {
            return em.find(SavingsGoal.class, id);
        }
    }
}