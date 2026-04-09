package com.qsaj.balarama.v1.service.impl;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.CategoryGroup;
import com.qsaj.balarama.v1.model.CategoryItem;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.Currency;
import com.qsaj.balarama.v1.model.enums.EventType;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.repository.CategoryGroupRepository;
import com.qsaj.balarama.v1.repository.CategoryItemRepository;
import com.qsaj.balarama.v1.repository.UserRepository;
import com.qsaj.balarama.v1.service.AuthService;
import com.qsaj.balarama.v1.service.EventService;
import com.qsaj.balarama.v1.util.ValidationUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;

public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final CategoryGroupRepository categoryGroupRepository;
    private final CategoryItemRepository categoryItemRepository;
    private final EventService eventService;

    @Inject
    public AuthServiceImpl(UserRepository userRepository,
                           CategoryGroupRepository categoryGroupRepository,
                           CategoryItemRepository categoryItemRepository,
                           EventService eventService) {
        this.userRepository = userRepository;
        this.categoryGroupRepository = categoryGroupRepository;
        this.categoryItemRepository = categoryItemRepository;
        this.eventService = eventService;
    }

    @Override
    public void register(String username, String email, String password) {
        ValidationUtil.validateEmail(email);

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username is already taken!");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email is already registered!");
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = User.builder()
                .username(username)
                .password(hashedPassword)
                .email(email)
                .savedAmount(BigDecimal.ZERO)
                .unallocatedSavings(BigDecimal.ZERO)
                .build();
        userRepository.save(user);

        createDefaultCategories(user);
        createSystemCategories(user);
    }

    private void createDefaultCategories(User user) {
        CategoryGroup incomeGroup = CategoryGroup.builder()
                .name("Income")
                .type(TransactionType.INCOME)
                .color("#27ae60")
                .user(user)
                .build();
        categoryGroupRepository.save(incomeGroup);

        CategoryGroup expenseGroup = CategoryGroup.builder()
                .name("Savings")
                .type(TransactionType.EXPENSE)
                .color("#e74c3c")
                .user(user)
                .build();
        categoryGroupRepository.save(expenseGroup);
    }

    private void createSystemCategories(User user) {
        CategoryGroup sysIncome = CategoryGroup.builder()
                .name("System")
                .type(TransactionType.INCOME)
                .color("#3498db")
                .user(user)
                .build();
        categoryGroupRepository.save(sysIncome);

        CategoryItem unallocatedIncome = CategoryItem.builder()
                .name("Unallocated Transfer")
                .group(sysIncome)
                .build();
        categoryItemRepository.save(unallocatedIncome);

        CategoryGroup sysExpense = CategoryGroup.builder()
                .name("System")
                .type(TransactionType.EXPENSE)
                .color("#3498db")
                .user(user)
                .build();
        categoryGroupRepository.save(sysExpense);

        CategoryItem unallocatedExpense = CategoryItem.builder()
                .name("Unallocated Transfer")
                .group(sysExpense)
                .build();
        categoryItemRepository.save(unallocatedExpense);
    }

    @Override
    public User authenticate(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> BCrypt.checkpw(password, user.getPassword()))
                .orElse(null);
    }

    @Override
    public boolean checkPassword(User user, String rawPassword) {
        return BCrypt.checkpw(rawPassword, user.getPassword());
    }

    @Override
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!checkPassword(user, currentPassword)) throw new IllegalArgumentException("Current password is incorrect.");
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        userRepository.update(user);
        eventService.logEvent(user, EventType.CATEGORY_UPDATED, "Password changed"); // Умовний тип
    }

    @Override
    public void updateProfile(User user, String newUsername, String newEmail) {
        ValidationUtil.validateEmail(newEmail);
        user.setUsername(newUsername);
        user.setEmail(newEmail);
        userRepository.update(user);
        eventService.logEvent(user, EventType.CATEGORY_UPDATED, "Profile updated (username/email)");
    }

    @Override
    public void updateCurrency(User user, Currency currency) {
        Currency oldCurrency = user.getCurrency();
        user.setCurrency(currency);
        userRepository.update(user);
        eventService.logEvent(user, EventType.CURRENCY_CHANGED,
                "Changed currency from " + (oldCurrency != null ? oldCurrency.name() : "USD") + " to " + currency.name());
    }
}