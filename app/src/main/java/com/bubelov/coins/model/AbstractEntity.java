package com.bubelov.coins.model;

import org.joda.time.DateTime;

import java.io.Serializable;

/**
 * Author: Igor Bubelov
 * Date: 07/05/15 12:38
 */

public abstract class AbstractEntity implements Serializable {
    private long id;

    private DateTime createdAt;

    private DateTime updatedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
    }

    public DateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(DateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
