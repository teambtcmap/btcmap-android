package com.bubelov.coins.event;

/**
 * Author: Igor Bubelov
 * Date: 03/05/15 19:49
 */

public class MerchantsSyncFinishedEvent {
    private boolean dataChanged;

    public MerchantsSyncFinishedEvent(boolean dataChanged) {
        this.dataChanged = dataChanged;
    }

    public boolean isDataChanged() {
        return dataChanged;
    }
}