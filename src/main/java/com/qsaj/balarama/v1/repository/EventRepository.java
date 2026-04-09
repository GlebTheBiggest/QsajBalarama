package com.qsaj.balarama.v1.repository;

import com.qsaj.balarama.v1.model.Event;
import com.qsaj.balarama.v1.model.User;

import java.util.List;

public interface EventRepository {
    void save(Event event);
    List<Event> findAllByUser(User user);
    List<Event> findRecentByUser(User user, int limit);
}