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

    /** The initial Gu value is also the reference point for attribute scaling. */
    public int initialGu() {
        return defaultGu;
    }

    public static int clampGu(int gu) {
        return Math.max(0, Math.min(100, gu));
    }

    public double baseHealth() {
        return 8.0D;
    }

    public double baseDamage() {
        return this == WINGED ? 8.0D : 2.0D;
    }

    public double baseSpeed() {
        return this == DETECTION ? 0.65D : 0.35D;
    }

    public double baseAttackSpeed() {
        return this == WINGED ? 4.0D : 2.0D;
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
