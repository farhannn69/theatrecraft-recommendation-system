package com.farhan.theatrecraft.core.model;

public enum Brand {
    BOSE("Bose"),
    SONOS("Sonos"),
    SAMSUNG("Samsung"),
    LG("LG"),
    JBL("JBL");

    private final String displayName;

    Brand(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
