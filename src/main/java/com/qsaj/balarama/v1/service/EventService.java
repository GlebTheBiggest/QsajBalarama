package com.qsaj.balarama.v1.service;

import com.qsaj.balarama.v1.model.Event;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.EventType;

import java.util.List;

public interface EventService {
    void logEvent(User user, EventType eventType, String message);
    List<Event> getRecentEvents(User user, int limit);
}