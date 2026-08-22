package com.example.typemoonaddon.worm;

import java.util.Locale;

public enum WormType {
    SILVERFISH("silverfish", 2),
    ENDERMITE("endermite", 5),
    WINGED("winged", 7),
    FIREPROOF("fireproof", 7),
    DETECTION("detection", 7);

    private final String id;
    private final int defaultGu;

    WormType(String id, int defaultGu) {
        this.id = id;
        this.defaultGu = defaultGu;
    }

    public String id() {
        return id;
    }

    public int defaultGu() {
        return defaultGu;
    }

    public static WormType byId(String id) {
        if (id == null) {
            return SILVERFISH;
        }
        for (WormType type : values()) {
            if (type.id.equals(id.toLowerCase(Locale.ROOT))) {
                return type;
            }
        }
        return SILVERFISH;
    }

    public boolean isSameSpecies(WormType other) {
        return this == other;
    }
}
