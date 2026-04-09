package com.qsaj.balarama.v1.service.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.qsaj.balarama.v1.model.CategoryGroup;
import com.qsaj.balarama.v1.model.CategoryItem;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.EventType;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.repository.CategoryGroupRepository;
import com.qsaj.balarama.v1.repository.CategoryItemRepository;
import com.qsaj.balarama.v1.service.EventService;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.stream.Collectors;

public class CategoryServiceImpl {
    private final CategoryGroupRepository groupRepo;
    private final CategoryItemRepository itemRepo;
    private final Provider<EntityManager> emp;
    private final EventService eventService;

    @Inject
    public CategoryServiceImpl(CategoryGroupRepository groupRepo,
                               CategoryItemRepository itemRepo,
                               Provider<EntityManager> emp,
                               EventService eventService) {
        this.groupRepo = groupRepo;
        this.itemRepo = itemRepo;
        this.emp = emp;
        this.eventService = eventService;
    }

    public void saveGroup(CategoryGroup group) {
        boolean isNew = (group.getId() == null);
        if (isNew) {
            groupRepo.save(group);
        } else {
            groupRepo.update(group);
        }
        eventService.logEvent(group.getUser(),
                isNew ? EventType.CATEGORY_CREATED : EventType.CATEGORY_UPDATED,
                (isNew ? "Created" : "Updated") + " category: " + group.getName() + " (" + group.getType() + ")");
    }

    public void deleteGroup(Long id) {
        CategoryGroup group = groupRepo.findById(id).orElse(null);
        if (group != null) {
            String name = group.getName();
            User user = group.getUser();
            EntityManager em = emp.get();
            em.getTransaction().begin();
            em.createQuery("DELETE FROM CategoryBudget b WHERE b.categoryGroup.id = :id").setParameter("id", id).executeUpdate();
            em.createQuery("DELETE FROM CategoryItem i WHERE i.group.id = :id").setParameter("id", id).executeUpdate();
            em.getTransaction().commit();
            groupRepo.delete(id);
            eventService.logEvent(user, EventType.CATEGORY_DELETED, "Deleted category: " + name);
        } else {
            groupRepo.delete(id);
        }
    }

    public List<CategoryGroup> getGroupsForUser(User user) {
        return groupRepo.findAllByUser(user);
    }

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
        boolean isNew = (item.getId() == null);
        if (isNew) {
            itemRepo.save(item);
        } else {
            itemRepo.update(item);
        }
        eventService.logEvent(item.getGroup().getUser(),
                isNew ? EventType.SUBCATEGORY_CREATED : EventType.SUBCATEGORY_UPDATED,
                (isNew ? "Created" : "Updated") + " subcategory: " + item.getName() + " in " + item.getGroup().getName());
    }

    public void deleteItem(Long id) {
        CategoryItem item = itemRepo.findById(id).orElse(null);
        if (item != null) {
            String name = item.getName();
            User user = item.getGroup().getUser();
            itemRepo.delete(id);
            eventService.logEvent(user, EventType.SUBCATEGORY_DELETED, "Deleted subcategory: " + name);
        } else {
            itemRepo.delete(id);
        }
    }

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