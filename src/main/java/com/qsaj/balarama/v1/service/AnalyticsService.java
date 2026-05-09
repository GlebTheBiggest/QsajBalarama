package com.qsaj.balarama.v1.service;

import com.qsaj.balarama.v1.model.Insight;
import com.qsaj.balarama.v1.model.User;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsService {
    List<Insight> generateInsights(User user, LocalDate startDate, LocalDate endDate);
}