package com.qsaj.balarama.v1.config;

import com.google.inject.AbstractModule;
import com.qsaj.balarama.v1.service.QuoteService;
import com.qsaj.balarama.v1.service.impl.QuoteServiceImpl;

public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(QuoteService.class).to(QuoteServiceImpl.class);
    }
}