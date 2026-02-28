package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;

import java.util.Set;

public final class GemCompatibilityService {
    private static final Set<String> WHITELIST = Set.of(
            "gravity_magic",
            "reinforcement",
            "projection"
    );

    private GemCompatibilityService() {
    }

    public static boolean isWhitelistedMagic(String magicId) {
        return WHITELIST.contains(magicId);
    }

    public static Set<String> getWhitelistedMagics() {
        return WHITELIST;
    }

    public static int calculateEngraveSuccessChance(GemQuality quality, GemType type, String magicId, double magicProficiency) {
        int base = switch (quality) {
            case POOR -> 60;
            case NORMAL -> 80;
            case HIGH -> 95;
        };

        int complexity = getComplexityModifier(magicId);
        int affinity = getAffinityModifier(type, magicId);
        int proficiency = (int) Math.round(Math.max(0.0, Math.min(100.0, magicProficiency)) * 0.15); // 0..15

        int chance = base + complexity + affinity + proficiency;
        if (chance < 5) return 5;
        return Math.min(99, chance);
    }

    private static int getComplexityModifier(String magicId) {
        return switch (magicId) {
            case "projection" -> -20;
            case "reinforcement" -> -10;
            case "gravity_magic" -> 0;
            default -> -30;
        };
    }

    private static int getAffinityModifier(GemType type, String magicId) {
        return switch (magicId) {
            case "gravity_magic" -> switch (type) {
                case BLACK_SHARD -> 20;
                case CYAN -> 10;
                case SAPPHIRE -> 5;
                case WHITE_GEMSTONE -> 0;
                case TOPAZ, EMERALD -> -5;
                case RUBY -> -10;
            };
            case "reinforcement" -> switch (type) {
                case TOPAZ -> 10;
                case EMERALD -> 5;
                case RUBY -> 3;
                case WHITE_GEMSTONE -> 0;
                case BLACK_SHARD -> 0;
                case CYAN, SAPPHIRE -> -5;
            };
            case "projection" -> switch (type) {
                case WHITE_GEMSTONE -> 10;
                case RUBY, SAPPHIRE -> 5;
                case BLACK_SHARD -> -10;
                case TOPAZ, EMERALD, CYAN -> -5;
            };
            default -> -10;
        };
    }
}
