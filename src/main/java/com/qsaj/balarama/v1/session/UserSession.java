package com.qsaj.balarama.v1.session;

import com.google.inject.Singleton;
import com.qsaj.balarama.v1.model.User;

@Singleton
public class UserSession {

    private User currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No user logged in!");
        }
        return currentUser;
    }

    public void clear() {
        this.currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}