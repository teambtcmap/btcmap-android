package com.bubelov.coins.data.api.coins;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Igor Bubelov
 */

public class UserParams {
    private final Map<String, Object> user;

    public UserParams(String email, String password, String firstName, String lastName) {
        user = new HashMap<>();
        user.put("email", email);
        user.put("password", password);
        user.put("first_name", firstName);
        user.put("last_name", lastName);
    }
}