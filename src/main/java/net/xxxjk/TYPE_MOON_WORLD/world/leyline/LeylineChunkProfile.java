package net.xxxjk.TYPE_MOON_WORLD.world.leyline;

public record LeylineChunkProfile(
        int concentration,
        long manaCapacity,
        double regenMultiplier,
        boolean gemTerrainBonusApplied
) {
}

