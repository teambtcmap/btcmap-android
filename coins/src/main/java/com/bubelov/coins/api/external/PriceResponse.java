package com.bubelov.coins.api.external;

import java.util.Date;

/**
 * Author: Igor Bubelov
 * Date: 12/05/15 22:18
 */

public class PriceResponse {
    private Date timestamp;

    private int price;

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
