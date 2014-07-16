package com.bubelov.coins.server.osm;

import com.google.gson.annotations.SerializedName;

/**
 * Author: Igor Bubelov
 * Date: 16/07/14 21:34
 */

public class Metadata {
    @SerializedName("timestamp_osm_base")
    private String timestampOsmBase;

    private String copyright;

    public String getTimestampOsmBase() {
        return timestampOsmBase;
    }

    public void setTimestampOsmBase(String timestampOsmBase) {
        this.timestampOsmBase = timestampOsmBase;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }
}
