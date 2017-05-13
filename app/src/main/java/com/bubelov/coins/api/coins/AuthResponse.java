package com.bubelov.coins.api.coins;

import com.bubelov.coins.model.User;

import java.util.List;

/**
 * @author Igor Bubelov
 */

public class AuthResponse {
    private User user;

    private String token;

    private List<String> errors;

    public User getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public List<String> getErrors() {
        return errors;
    }
}