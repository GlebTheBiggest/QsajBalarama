package com.qsaj.balarama.v1.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.qsaj.balarama.v1.repository.*;
import com.qsaj.balarama.v1.repository.impl.*;
import com.qsaj.balarama.v1.service.*;
import com.qsaj.balarama.v1.service.impl.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class AppModule extends AbstractModule {

    @Override
    protected void configure() {
        // Сервіси
        bind(QuoteService.class).to(QuoteServiceImpl.class);
        bind(AuthService.class).to(AuthServiceImpl.class);
        bind(FinanceService.class).to(FinanceServiceImpl.class);
        bind(SavingsService.class).to(SavingsServiceImpl.class);
        bind(NavigationService.class).to(NavigationServiceImpl.class).in(Singleton.class);
        bind(CategoryServiceImpl.class).in(Singleton.class);

        // НОВИЙ СЕРВІС БЮДЖЕТУВАННЯ
        bind(BudgetService.class).to(BudgetServiceImpl.class).in(Singleton.class);

        // Репозиторії
        bind(UserRepository.class).to(UserRepositoryImpl.class);
        bind(TransactionRepository.class).to(TransactionRepositoryImpl.class);
        bind(SavingsGoalRepository.class).to(SavingsGoalRepositoryImpl.class);
        bind(CategoryGroupRepository.class).to(CategoryGroupRepositoryImpl.class).in(Singleton.class);
        bind(CategoryItemRepository.class).to(CategoryItemRepositoryImpl.class).in(Singleton.class);

        // НОВІ РЕПОЗИТОРІЇ БЮДЖЕТУВАННЯ
        bind(BudgetPeriodRepository.class).to(BudgetPeriodRepositoryImpl.class).in(Singleton.class);
        bind(CategoryBudgetRepository.class).to(CategoryBudgetRepositoryImpl.class).in(Singleton.class);

        // Додати в configure():
        bind(EventRepository.class).to(EventRepositoryImpl.class);
        bind(EventService.class).to(EventServiceImpl.class);
    }

    @Provides
    @Singleton
    public EntityManagerFactory provideEntityManagerFactory() {
        return Persistence.createEntityManagerFactory("balarama-unit");
    }

    @Provides
    public EntityManager provideEntityManager(EntityManagerFactory emf) {
        return emf.createEntityManager();
    }
}