package com.qsaj.balarama.v1.service.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.qsaj.balarama.v1.model.CategoryGroup;
import com.qsaj.balarama.v1.model.CategoryItem;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.repository.CategoryGroupRepository;
import com.qsaj.balarama.v1.repository.CategoryItemRepository;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.stream.Collectors;

public class CategoryServiceImpl {
    private final CategoryGroupRepository groupRepo;
    private final CategoryItemRepository itemRepo;
    private final Provider<EntityManager> emp;

    @Inject
    public CategoryServiceImpl(CategoryGroupRepository groupRepo, CategoryItemRepository itemRepo, Provider<EntityManager> emp) {
        this.groupRepo = groupRepo;
        this.itemRepo = itemRepo;
        this.emp = emp;
    }

    public void saveGroup(CategoryGroup group) {
        if (group.getId() == null) groupRepo.save(group); else groupRepo.update(group);
    }

    public void deleteGroup(Long id) {
        EntityManager em = emp.get();
        em.getTransaction().begin();
        em.createQuery("DELETE FROM CategoryBudget b WHERE b.categoryGroup.id = :id").setParameter("id", id).executeUpdate();
        em.createQuery("DELETE FROM CategoryItem i WHERE i.group.id = :id").setParameter("id", id).executeUpdate();
        em.getTransaction().commit();
        groupRepo.delete(id);
    }

    public List<CategoryGroup> getGroupsForUser(User user) {
        return groupRepo.findAllByUser(user);
    }

    // НОВИЙ МЕТОД: повертає групи, видимі для користувача (виключає "System")
    public List<CategoryGroup> getUserVisibleGroups(User user) {
        return groupRepo.findAllByUser(user).stream()
                .filter(g -> !"System".equalsIgnoreCase(g.getName()))
                .collect(Collectors.toList());
    }

    public List<CategoryGroup> getGroupsForUserByType(User user, TransactionType type) {
        return groupRepo.findAllByUserAndType(user, type).stream()
                .filter(g -> !"System".equalsIgnoreCase(g.getName()))
                .collect(Collectors.toList());
    }

    public void saveItem(CategoryItem item) {
        if (item.getId() == null) itemRepo.save(item); else itemRepo.update(item);
    }

    public void deleteItem(Long id) { itemRepo.delete(id); }

    public List<CategoryItem> getItemsForGroup(Long groupId) {
        return itemRepo.findAllByGroupId(groupId);
    }

    public CategoryItem findItemByGroupAndName(CategoryGroup group, String itemName) {
        return itemRepo.findAllByGroupId(group.getId()).stream()
                .filter(i -> i.getName().equalsIgnoreCase(itemName))
                .findFirst().orElse(null);
    }

    public void deleteItemsByGroupNameAndItemName(User user, String groupName, String itemName) {
        EntityManager em = emp.get();
        em.getTransaction().begin();
        em.createQuery("DELETE FROM CategoryItem i WHERE i.name = :itemName AND i.group.name = :groupName AND i.group.user = :user")
                .setParameter("itemName", itemName).setParameter("groupName", groupName).setParameter("user", user)
                .executeUpdate();
        em.getTransaction().commit();
    }
}