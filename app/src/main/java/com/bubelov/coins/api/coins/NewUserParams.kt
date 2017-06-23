package com.bubelov.coins.api.coins

import java.util.HashMap

/**
 * @author Igor Bubelov
 */

class NewUserParams(email: String, password: String, firstName: String, lastName: String) {
    private val user: MutableMap<String, Any>

    init {
        user = HashMap<String, Any>().apply {
            put("email", email)
            put("password", password)
            put("first_name", firstName)
            put("last_name", lastName)
        }
    }
}