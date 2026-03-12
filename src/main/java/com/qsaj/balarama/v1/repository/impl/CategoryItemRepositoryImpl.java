package com.qsaj.balarama.v1.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.qsaj.balarama.v1.model.CategoryItem;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.repository.CategoryItemRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

public class CategoryItemRepositoryImpl implements CategoryItemRepository {

    private final Provider<EntityManager> entityManagerProvider;

    @Inject
    public CategoryItemRepositoryImpl(Provider<EntityManager> entityManagerProvider) {
        this.entityManagerProvider = entityManagerProvider;
    }

    @Override
    public void save(CategoryItem item) {
        EntityTransaction transaction = null;
        try (EntityManager em = entityManagerProvider.get()) {
            transaction = em.getTransaction();
            transaction.begin();
            em.persist(item);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            throw e;
        }
    }

    @Override
    public void update(CategoryItem item) {
        EntityTransaction transaction = null;
        try (EntityManager em = entityManagerProvider.get()) {
            transaction = em.getTransaction();
            transaction.begin();
            em.merge(item);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            throw e;
        }
    }

    @Override
    public void delete(Long id) {
        EntityTransaction transaction = null;
        try (EntityManager em = entityManagerProvider.get()) {
            transaction = em.getTransaction();
            transaction.begin();
            CategoryItem item = em.find(CategoryItem.class, id);
            if (item != null) {
                em.remove(item);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            throw e;
        }
    }

    @Override
    public Optional<CategoryItem> findById(Long id) {
        try (EntityManager em = entityManagerProvider.get()) {
            return Optional.ofNullable(em.find(CategoryItem.class, id));
        }
    }

    @Override
    public List<CategoryItem> findAllByGroupId(Long groupId) {
        try (EntityManager em = entityManagerProvider.get()) {
            TypedQuery<CategoryItem> query = em.createQuery(
                    "SELECT i FROM CategoryItem i WHERE i.group.id = :groupId", CategoryItem.class);
            query.setParameter("groupId", groupId);
            return query.getResultList();
        }
    }

    @Override
    public List<CategoryItem> findAllByUserAndType(User user, TransactionType type) {
        try (EntityManager em = entityManagerProvider.get()) {
            TypedQuery<CategoryItem> query = em.createQuery(
                    "SELECT ci FROM CategoryItem ci WHERE ci.group.user = :user AND ci.group.type = :type", CategoryItem.class);
            query.setParameter("user", user);
            query.setParameter("type", type);
            return query.getResultList();
        }
    }

    @Override
    public Optional<CategoryItem> findByUserAndName(User user, String name) {
        try (EntityManager em = entityManagerProvider.get()) {
            TypedQuery<CategoryItem> query = em.createQuery(
                    "SELECT ci FROM CategoryItem ci WHERE ci.group.user = :user AND ci.name = :name", CategoryItem.class);
            query.setParameter("user", user);
            query.setParameter("name", name);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}