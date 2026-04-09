package com.qsaj.balarama.v1.service.impl;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.Event;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.EventType;
import com.qsaj.balarama.v1.repository.EventRepository;
import com.qsaj.balarama.v1.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    @Inject
    public EventServiceImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void logEvent(User user, EventType eventType, String message) {
        Event event = Event.builder()
                .user(user)
                .timestamp(LocalDateTime.now())
                .eventType(eventType)
                .message(message)
                .build();
        eventRepository.save(event);
    }

    @Override
    public List<Event> getRecentEvents(User user, int limit) {
        return eventRepository.findRecentByUser(user, limit);
    }
}