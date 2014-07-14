package com.bubelov.coins.server.osm;

import java.util.Map;

/**
 * Author: Igor Bubelov
 * Date: 12/07/14 14:02
 */

public class Element {
    private long id;

    private String type;

    private double lat;

    private double lon;

    private Map<String, String> tags;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
}
