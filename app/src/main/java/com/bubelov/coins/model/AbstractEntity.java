package com.bubelov.coins.model;

import java.io.Serializable;

/**
 * @author Igor Bubelov
 */

public abstract class AbstractEntity implements Serializable {
    protected long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}