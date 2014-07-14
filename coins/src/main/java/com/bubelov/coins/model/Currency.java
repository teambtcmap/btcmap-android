package com.bubelov.coins.model;

/**
 * Author: Igor Bubelov
 * Date: 03/07/14 22:54
 */

public class Currency {
    private long id;

    private String name;

    private String code;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}