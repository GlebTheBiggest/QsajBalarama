package com.qsaj.balarama.v1.repository;

import com.qsaj.balarama.v1.model.CategoryItem;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import java.util.List;
import java.util.Optional;

public interface CategoryItemRepository {
    void save(CategoryItem item);
    void update(CategoryItem item);
    void delete(Long id);
    Optional<CategoryItem> findById(Long id);
    List<CategoryItem> findAllByGroupId(Long groupId);
    // New methods
    List<CategoryItem> findAllByUserAndType(User user, TransactionType type);
    Optional<CategoryItem> findByUserAndName(User user, String name);
}