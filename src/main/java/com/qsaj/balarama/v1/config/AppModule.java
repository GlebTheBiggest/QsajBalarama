package com.qsaj.balarama.v1.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.qsaj.balarama.v1.repository.UserRepository;
import com.qsaj.balarama.v1.repository.impl.UserRepositoryImpl;
import com.qsaj.balarama.v1.service.AuthService;
import com.qsaj.balarama.v1.service.QuoteService;
import com.qsaj.balarama.v1.service.impl.AuthServiceImpl;
import com.qsaj.balarama.v1.service.impl.QuoteServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class AppModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(QuoteService.class).to(QuoteServiceImpl.class);
        // Сервіси
        bind(QuoteService.class).to(QuoteServiceImpl.class);
        bind(AuthService.class).to(AuthServiceImpl.class); // <--- НОВЕ

        // Репозиторії
        bind(UserRepository.class).to(UserRepositoryImpl.class); // <--- НОВЕ
    }

    // 1. Створюємо Фабрику (вона важка, тому робимо її Singleton - одну на всю програму)
    @Provides
    @Singleton
    public EntityManagerFactory provideEntityManagerFactory() {
        // "balarama-unit" має співпадати з назвою в persistence.xml
        return Persistence.createEntityManagerFactory("balarama-unit");
    }

    // 2. Створюємо Менеджера (він легкий, створюється для кожного запиту)
    @Provides
    public EntityManager provideEntityManager(EntityManagerFactory emf) {
        return emf.createEntityManager();
    }
}