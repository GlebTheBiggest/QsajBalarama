package com.qsaj.balarama.v1.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.qsaj.balarama.v1.model.CategoryGroup;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.repository.CategoryGroupRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

public class CategoryGroupRepositoryImpl implements CategoryGroupRepository {

    private final Provider<EntityManager> entityManagerProvider;

    @Inject
    public CategoryGroupRepositoryImpl(Provider<EntityManager> entityManagerProvider) {
        this.entityManagerProvider = entityManagerProvider;
    }

    @Override
    public void save(CategoryGroup group) {
        EntityManager em = entityManagerProvider.get();
        EntityTransaction tx = em.getTransaction();
        boolean isNewTx = !tx.isActive();
        if (isNewTx) tx.begin();
        try {
            em.persist(group);
            if (isNewTx) tx.commit();
        } catch (Exception e) {
            if (isNewTx && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    @Override
    public void update(CategoryGroup group) {
        EntityManager em = entityManagerProvider.get();
        EntityTransaction tx = em.getTransaction();
        boolean isNewTx = !tx.isActive();
        if (isNewTx) tx.begin();
        try {
            em.merge(group);
            if (isNewTx) tx.commit();
        } catch (Exception e) {
            if (isNewTx && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    @Override
    public void delete(Long id) {
        EntityManager em = entityManagerProvider.get();
        EntityTransaction tx = em.getTransaction();
        boolean isNewTx = !tx.isActive();
        if (isNewTx) tx.begin();
        try {
            CategoryGroup group = em.find(CategoryGroup.class, id);
            if (group != null) {
                em.remove(group);
            }
            if (isNewTx) tx.commit();
        } catch (Exception e) {
            if (isNewTx && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    @Override
    public Optional<CategoryGroup> findById(Long id) {
        EntityManager em = entityManagerProvider.get();
        return Optional.ofNullable(em.find(CategoryGroup.class, id));
    }

    @Override
    public List<CategoryGroup> findAllByUser(User user) {
        EntityManager em = entityManagerProvider.get();
        TypedQuery<CategoryGroup> query = em.createQuery(
                "SELECT g FROM CategoryGroup g WHERE g.user = :user", CategoryGroup.class);
        query.setParameter("user", user);
        return query.getResultList();
    }

    @Override
    public List<CategoryGroup> findAllByUserAndType(User user, TransactionType type) {
        EntityManager em = entityManagerProvider.get();
        TypedQuery<CategoryGroup> query = em.createQuery(
                "SELECT g FROM CategoryGroup g WHERE g.user = :user AND g.type = :type", CategoryGroup.class);
        query.setParameter("user", user);
        query.setParameter("type", type);
        return query.getResultList();
    }
}