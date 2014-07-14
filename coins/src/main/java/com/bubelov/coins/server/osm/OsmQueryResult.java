package com.bubelov.coins.server.osm;

import java.util.ArrayList;

/**
 * Author: Igor Bubelov
 * Date: 02/06/14 21:40
 */

public class OsmQueryResult {
    private String version;
    private String generator;
    private ArrayList<Element> elements;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGenerator() {
        return generator;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }

    public ArrayList<Element> getElements() {
        return elements;
    }

    public void setElements(ArrayList<Element> elements) {
        this.elements = elements;
    }
}
