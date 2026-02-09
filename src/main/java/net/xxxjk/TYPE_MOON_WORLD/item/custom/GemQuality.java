package net.xxxjk.TYPE_MOON_WORLD.item.custom;

public enum GemQuality {
    POOR(50, 0.8f, "poor"),
    NORMAL(100, 1.0f, "normal"),
    HIGH(200, 1.5f, "high");

    private final int capacity;
    private final float effectMultiplier; // For scaling effects
    private final String name;

    GemQuality(int capacity, float effectMultiplier, String name) {
        this.capacity = capacity;
        this.effectMultiplier = effectMultiplier;
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public float getEffectMultiplier() {
        return effectMultiplier;
    }
    
    public String getName() {
        return name;
    }
}
