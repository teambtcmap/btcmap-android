package com.bubelov.coins.api.coins;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Igor Bubelov
 */

public class NewUserParams {
    private final Map<String, Object> user;

    public NewUserParams(String email, String password, String firstName, String lastName) {
        user = new HashMap<>();
        user.put("email", email);
        user.put("password", password);
        user.put("first_name", firstName);
        user.put("last_name", lastName);
    }
}