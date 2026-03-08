package net.xxxjk.TYPE_MOON_WORLD.magic;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Hidden magic taxonomy (backend only).
 * This classification is intentionally not exposed in current GUI pages.
 */
public final class MagicClassification {
    public enum ManaCostType {
        ONE_TIME,
        SUSTAINED_OR_INDIRECT
    }

    public enum MagicSchoolType {
        NONE,
        NORDIC
    }

    private static final Map<String, ManaCostType> MAGIC_COST_TYPES = Map.ofEntries(
            Map.entry("ruby_throw", ManaCostType.ONE_TIME),
            Map.entry("sapphire_throw", ManaCostType.ONE_TIME),
            Map.entry("emerald_use", ManaCostType.ONE_TIME),
            Map.entry("topaz_throw", ManaCostType.ONE_TIME),
            Map.entry("cyan_throw", ManaCostType.ONE_TIME),

            Map.entry("ruby_flame_sword", ManaCostType.ONE_TIME),
            Map.entry("sapphire_winter_frost", ManaCostType.ONE_TIME),
            Map.entry("emerald_winter_river", ManaCostType.ONE_TIME),
            Map.entry("topaz_reinforcement", ManaCostType.ONE_TIME),
            Map.entry("cyan_wind", ManaCostType.ONE_TIME),

            Map.entry("jewel_magic_shoot", ManaCostType.ONE_TIME),
            Map.entry("jewel_magic_release", ManaCostType.ONE_TIME),
            Map.entry("jewel_random_shoot", ManaCostType.ONE_TIME),
            Map.entry("jewel_machine_gun", ManaCostType.ONE_TIME),
            Map.entry("gandr_machine_gun", ManaCostType.ONE_TIME),

            Map.entry("projection", ManaCostType.ONE_TIME),
            Map.entry("structural_analysis", ManaCostType.ONE_TIME),
            Map.entry("broken_phantasm", ManaCostType.ONE_TIME),
            Map.entry("gravity_magic", ManaCostType.ONE_TIME),
            Map.entry("gander", ManaCostType.SUSTAINED_OR_INDIRECT),

            Map.entry("reinforcement", ManaCostType.SUSTAINED_OR_INDIRECT),
            Map.entry("reinforcement_self", ManaCostType.SUSTAINED_OR_INDIRECT),
            Map.entry("reinforcement_other", ManaCostType.SUSTAINED_OR_INDIRECT),
            Map.entry("reinforcement_item", ManaCostType.SUSTAINED_OR_INDIRECT),

            Map.entry("unlimited_blade_works", ManaCostType.SUSTAINED_OR_INDIRECT),
            Map.entry("sword_barrel_full_open", ManaCostType.SUSTAINED_OR_INDIRECT)
    );

    private static final Set<String> ALL_MAGIC_IDS = Collections.unmodifiableSet(MAGIC_COST_TYPES.keySet());
    private static final Map<String, MagicSchoolType> MAGIC_SCHOOL_TYPES = Map.ofEntries(
            Map.entry("gander", MagicSchoolType.NORDIC),
            Map.entry("jewel_machine_gun", MagicSchoolType.NORDIC),
            Map.entry("gandr_machine_gun", MagicSchoolType.NORDIC)
    );

    private MagicClassification() {
    }

    public static boolean isKnownMagic(String magicId) {
        return magicId != null && MAGIC_COST_TYPES.containsKey(magicId);
    }

    public static ManaCostType getManaCostType(String magicId) {
        return MAGIC_COST_TYPES.getOrDefault(magicId, ManaCostType.ONE_TIME);
    }

    public static Set<String> getAllMagicIds() {
        return ALL_MAGIC_IDS;
    }

    public static MagicSchoolType getSchoolType(String magicId) {
        return MAGIC_SCHOOL_TYPES.getOrDefault(magicId, MagicSchoolType.NONE);
    }
}
