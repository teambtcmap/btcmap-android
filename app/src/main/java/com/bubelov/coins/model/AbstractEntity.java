package com.bubelov.coins.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Author: Igor Bubelov
 * Date: 07/05/15 12:38
 */

public abstract class AbstractEntity implements Serializable {
    protected long id;

    protected Date updatedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
