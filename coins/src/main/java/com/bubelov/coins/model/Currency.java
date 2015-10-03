package com.bubelov.coins.model;

/**
 * Author: Igor Bubelov
 * Date: 03/07/14 22:54
 */

public class Currency extends AbstractEntity {
    private String name;

    private String code;

    private boolean crypto;

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

    public boolean isCrypto() {
        return crypto;
    }

    public void setCrypto(boolean crypto) {
        this.crypto = crypto;
    }
}