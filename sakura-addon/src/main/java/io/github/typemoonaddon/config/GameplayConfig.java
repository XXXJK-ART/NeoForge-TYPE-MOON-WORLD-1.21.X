package io.github.typemoonaddon.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class GameplayConfig {
    public static final int DATA_VERSION = 11;
    public static final int SHADOW_ART_RIBBON_COUNT = 10;
    public static final float SHADOW_ART_RIBBON_HEALTH = 100.0F;
    public static final int SHADOW_ART_RIBBON_RESPAWN_TICKS = 30 * 20;
    public static final double SHADOW_ART_MANA_PER_RIBBON_SECOND = 1.0D;
    public static final double SHADOW_ART_IDLE_RADIUS = 5.0D;
    public static final double SHADOW_ART_ROOT_RADIUS = 2.0D;
    public static final double SHADOW_ART_ATTACK_RADIUS = 50.0D;
    public static final double SHADOW_ART_MAX_LENGTH = 100.0D;
    public static final int SHADOW_ART_TARGET_SCAN_TICKS = 2;
    public static final int SHADOW_ART_ATTACK_COOLDOWN_TICKS = 6;
    public static final int SHADOW_ART_STRIKE_TICKS = 3;
    public static final int SHADOW_ART_HOLD_RIBBON_COUNT = 3;
    public static final int EMIYA_SHADOW_PIN_ANIMATION_DELAY_TICKS = 2 * 20;
    public static final int EMIYA_SHADOW_CHANT_LINE_TICKS = 2 * 20;
    public static final int EMIYA_SHADOW_CHANT_TICKS = 156;
    public static final int EMIYA_SHADOW_RHO_AIAS_SEQUENCE_TICKS = 85;
    public static final int EMIYA_SHADOW_RHO_AIAS_PERSIST_TICKS = 5 * 20;
    public static final int EMIYA_SHADOW_RHO_AIAS_TOTAL_TICKS = EMIYA_SHADOW_RHO_AIAS_SEQUENCE_TICKS
        + EMIYA_SHADOW_RHO_AIAS_PERSIST_TICKS;
    public static final float EMIYA_SHADOW_POST_RHO_AIAS_SPIRITUAL_DAMAGE_PERCENT = 70.0F;
    public static final int SHADOW_ART_LIFT_TICKS = 18;
    public static final int SHADOW_ART_THROW_TICKS = 22;
    public static final int SHADOW_ART_RETRACT_TICKS = 10;
    public static final double SHADOW_ART_LIFT_HEIGHT = 3.4D;
    public static final double SHADOW_ART_PIN_HEIGHT = 3.2D;
    public static final double SHADOW_ART_MODEL_WIDTH = 2.6D / 16.0D;
    public static final double SHADOW_ART_MODEL_SEGMENT_LENGTH = SHADOW_ART_MODEL_WIDTH * 3.8D / 2.6D;
    public static final int SHADOW_ART_PARTICLE_INTERVAL_TICKS = 10;
    public static final int SHADOW_ART_BLACK_PARTICLES = 8;
    public static final int SHADOW_ART_RED_PARTICLES = 2;
    public static final float SHADOW_ART_ATTACK_DAMAGE = 8.0F;
    public static final float SHADOW_ART_PROJECTILE_BLOCK_DAMAGE = 12.0F;
    public static final double SHADOW_ART_PROJECTILE_SCAN_RADIUS = 12.0D;
    public static final double SHADOW_ART_PROJECTILE_HIT_RADIUS_SQR = 2.25D;
    public static final float BLACK_SHADOW_POLLUTION_PER_SECOND = 0.01F;
    public static final float BLACK_SHADOW_POLLUTION_PER_SERVANT_KILL = 0.10F;
    public static final int BLACK_SHADOW_MUD_RADIUS = 50;
    public static final int SHADOW_ART_PREFERRED_MUD_MIN_RADIUS = 20;
    public static final int SHADOW_ART_PREFERRED_MUD_MAX_RADIUS = 30;
    public static final int MAGIC_OUTPUT_CHARGE_TICKS = 17 * 20;
    public static final int BLACK_SHADOW_MAGIC_OUTPUT_DISMISS_DELAY_TICKS = 3 * 20;
    public static final float MAGIC_OUTPUT_ORB_START_RADIUS = 1.0F;
    public static final float MAGIC_OUTPUT_ORB_END_RADIUS = 60.0F;
    public static final double MAGIC_OUTPUT_RADIUS = 60.0D;
    public static final int MAGIC_OUTPUT_SHOCKWAVE_TICKS = 5 * 20;
    public static final float MAGIC_OUTPUT_TERRAIN_EXPLOSION_POWER = 24.0F;
    public static final float MAGIC_OUTPUT_CENTER_DAMAGE = 5_000.0F;
    public static final float MAGIC_OUTPUT_EDGE_DAMAGE = 100.0F;
    public static final float MAGIC_OUTPUT_SHOCKWAVE_DAMAGE = 80.0F;
    public static final int MAGIC_OUTPUT_SHOCKWAVE_BLOCK_BUDGET = 256;
    public static final float SPIRIT_ORIGIN_DAMAGE_PER_SECOND = 2.0F;
    public static final double SPIRIT_ORIGIN_MANA_PER_PERCENT = 10.0D;
    public static final int SPIRIT_ORIGIN_MAX_DEATH_TICKS = 2 * 60 * 20;
    public static final int MENU_CAPACITY = 54;
    public static final int ITEM_ABSORPTION_TICKS = 20;
    public static final int BLOCK_ABSORPTION_TICKS = 3 * 20;
    public static final int PROTECTION_DURATION_TICKS = 10 * 20;
    public static final float PROTECTION_MAX_SHIELD = 2_000.0F;
    public static final int PROTECTION_COOLDOWN_TICKS = 10 * 20;
    public static final int PROTECTION_RECOVERY_DELAY_TICKS = 10 * 20;
    public static final float PROTECTION_RECOVERY_PER_TICK = PROTECTION_MAX_SHIELD / (10.0F * 20.0F);
    public static final int GRAIL_EROSION_FINAL_DELAY_TICKS = 3 * 60 * 20;
    public static final int CURSED_ARMOR_TRANSITION_TICKS = 3 * 20;
    public static final float CURSED_ARMOR_POINTS = 10.0F;
    public static final float CURSED_ARMOR_SERVANT_MULTIPLIER = 10.0F;
    public static final int SHADOW_DAMAGE_INTERVAL_TICKS = 20;
    public static final int SHADOW_DISSOLUTION_TICKS = 3 * 20;
    public static final float SHADOW_DAMAGE_PER_SECOND = 30.0F;
    public static final double ABSORPTION_MANA_DRAIN_PER_SECOND = 30.0D;
    public static final float GRAIL_SERVANT_DAMAGE_MULTIPLIER = 10.0F;
    public static final int GRAIL_FAKE_DEATH_TICKS = 5 * 20;
    public static final double PROFICIENCY_GAIN_PER_PRACTICE = 0.2D;
    public static final double PROTECTION_ZONE_SIZE = 2.0D;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue MAX_RANGE;
    public static final ModConfigSpec.DoubleValue MANA_COST_MULTIPLIER;
    public static final ModConfigSpec.IntValue MAX_SPACE_PAGES;
    public static final ModConfigSpec.DoubleValue LIVING_ABSORPTION_RANGE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("imaginary_absorption");
        MAX_RANGE = builder.comment("Maximum server-validated targeting range.")
            .defineInRange("maxRange", 50.0D, 1.0D, 64.0D);
        MANA_COST_MULTIPLIER = builder.defineInRange("itemManaCostMultiplier", 1.0D, 0.0D, 100.0D);
        MAX_SPACE_PAGES = builder.comment("Maximum automatically-created Imaginary Space pages (54 slots each).")
            .defineInRange("maxSpacePages", 256, 1, 1024);
        builder.pop();
        builder.push("living_absorption");
        LIVING_ABSORPTION_RANGE = builder.comment("Maximum spherical range for living Imaginary Absorption targets.")
            .defineInRange("range", 50.0D, 1.0D, 50.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private GameplayConfig() {
    }
}
