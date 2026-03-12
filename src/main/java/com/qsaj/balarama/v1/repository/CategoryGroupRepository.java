package com.qsaj.balarama.v1.repository;

import com.qsaj.balarama.v1.model.CategoryGroup;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import java.util.List;
import java.util.Optional;

public interface CategoryGroupRepository {
    void save(CategoryGroup group);
    void update(CategoryGroup group);
    void delete(Long id);
    Optional<CategoryGroup> findById(Long id);               // змінено на Optional
    List<CategoryGroup> findAllByUser(User user);
    List<CategoryGroup> findAllByUserAndType(User user, TransactionType type);
}