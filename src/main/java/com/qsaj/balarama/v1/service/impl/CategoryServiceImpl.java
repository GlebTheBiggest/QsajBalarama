package com.qsaj.balarama.v1.service.impl;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.CategoryGroup;
import com.qsaj.balarama.v1.model.CategoryItem;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.repository.CategoryGroupRepository;
import com.qsaj.balarama.v1.repository.CategoryItemRepository;

import java.util.List;

public class CategoryServiceImpl {
    private final CategoryGroupRepository groupRepo;
    private final CategoryItemRepository itemRepo;

    @Inject
    public CategoryServiceImpl(CategoryGroupRepository groupRepo, CategoryItemRepository itemRepo) {
        this.groupRepo = groupRepo;
        this.itemRepo = itemRepo;
    }

    // Groups
    public void saveGroup(CategoryGroup group) {
        if (group.getId() == null) {
            groupRepo.save(group);
        } else {
            groupRepo.update(group);
        }
    }

    public void deleteGroup(Long id) {
        groupRepo.delete(id);
    }

    public List<CategoryGroup> getGroupsForUser(User user) {
        return groupRepo.findAllByUser(user);
    }

    public List<CategoryGroup> getGroupsForUserByType(User user, TransactionType type) {
        return groupRepo.findAllByUserAndType(user, type);
    }

    // Items
    public void saveItem(CategoryItem item) {
        if (item.getId() == null) {
            itemRepo.save(item);
        } else {
            itemRepo.update(item);
        }
    }

    public void deleteItem(Long id) {
        itemRepo.delete(id);
    }

    public List<CategoryItem> getItemsForGroup(Long groupId) {
        return itemRepo.findAllByGroupId(groupId);
    }

    // New methods for fetching items by user and type
    public List<CategoryItem> getItemsForUserByType(User user, TransactionType type) {
        return itemRepo.findAllByUserAndType(user, type);
    }

    public CategoryItem findItemByUserAndName(User user, String itemName) {
        return itemRepo.findByUserAndName(user, itemName).orElse(null);
    }
}