package net.xxxjk.TYPE_MOON_WORLD.constants;

public class MagicConstants {
    // Ruby Throw Magic
    public static final float RUBY_EXPLOSION_RADIUS = 5.0F;
    public static final float RUBY_THROW_VELOCITY = 1.5F;
    public static final float RUBY_THROW_INACCURACY = 1.0F;
    public static final int RUBY_LIFETIME_TICKS = 200; // 10 seconds

    // Sapphire Throw Magic
    public static final float SAPPHIRE_THROW_VELOCITY = 1.5F;
    public static final float SAPPHIRE_THROW_INACCURACY = 1.0F;
    public static final int SAPPHIRE_ICE_RADIUS = 3;
    public static final int SAPPHIRE_ICE_DURATION = 100; // 5 seconds (20 ticks * 5)
    public static final int SAPPHIRE_DEBUFF_DURATION = 100; // 5 seconds

    // Emerald Use Magic
    public static final int EMERALD_WALL_DURATION = 160; // 8 seconds (range 8-10s)
    public static final int EMERALD_WALL_WIDTH = 3;
    public static final int EMERALD_WALL_HEIGHT = 3;
    public static final int EMERALD_WALL_DISTANCE = 4; // Moved from 2 to 4 (2 blocks further)

    // Topaz Throw Magic
    public static final float TOPAZ_THROW_VELOCITY = 2.0F;
    public static final float TOPAZ_THROW_INACCURACY = 1.0F;
    public static final int TOPAZ_DEBUFF_DURATION = 200; // 10 seconds
    public static final double TOPAZ_EFFECT_RADIUS = 5.0;

    // Cyan Wind Magic
    public static final float CYAN_WIND_RADIUS = 4.0F;
    public static final int CYAN_WIND_DURATION = 100; // 5 seconds

    // Translation Keys
    public static final String KEY_MAGIC_RUBY_THROW = "gui.typemoonworld.magic.ruby_throw";
    public static final String KEY_MAGIC_RUBY_THROW_SELECTED = "gui.typemoonworld.magic.ruby_throw.selected";
    public static final String KEY_MAGIC_RUBY_THROW_SHORT = "gui.typemoonworld.magic.ruby_throw.short";
    public static final String MSG_MAGIC_RUBY_THROW_NEED_GEM = "message.typemoonworld.magic.ruby_throw.need_gem";
    
    public static final String KEY_MAGIC_SAPPHIRE_THROW = "gui.typemoonworld.magic.sapphire_throw";
    public static final String KEY_MAGIC_SAPPHIRE_THROW_SELECTED = "gui.typemoonworld.magic.sapphire_throw.selected";
    public static final String KEY_MAGIC_SAPPHIRE_THROW_SHORT = "gui.typemoonworld.magic.sapphire_throw.short";
    public static final String MSG_MAGIC_SAPPHIRE_THROW_NEED_GEM = "message.typemoonworld.magic.sapphire_throw.need_gem";

    public static final String KEY_MAGIC_EMERALD_USE = "gui.typemoonworld.magic.emerald_use";
    public static final String KEY_MAGIC_EMERALD_USE_SELECTED = "gui.typemoonworld.magic.emerald_use.selected";
    public static final String KEY_MAGIC_EMERALD_USE_SHORT = "gui.typemoonworld.magic.emerald_use.short";
    public static final String MSG_MAGIC_EMERALD_USE_NEED_GEM = "message.typemoonworld.magic.emerald_use.need_gem";

    public static final String KEY_MAGIC_TOPAZ_THROW = "gui.typemoonworld.magic.topaz_throw";
    public static final String KEY_MAGIC_TOPAZ_THROW_SELECTED = "gui.typemoonworld.magic.topaz_throw.selected";
    public static final String KEY_MAGIC_TOPAZ_THROW_SHORT = "gui.typemoonworld.magic.topaz_throw.short";
    public static final String MSG_MAGIC_TOPAZ_THROW_NEED_GEM = "message.typemoonworld.magic.topaz_throw.need_gem";

    // Advanced Magic Keys
    public static final String KEY_MAGIC_RUBY_FLAME_SWORD = "gui.typemoonworld.magic.ruby_flame_sword";
    public static final String KEY_MAGIC_RUBY_FLAME_SWORD_SELECTED = "gui.typemoonworld.magic.ruby_flame_sword.selected";
    public static final String KEY_MAGIC_RUBY_FLAME_SWORD_SHORT = "gui.typemoonworld.magic.ruby_flame_sword.short";
    public static final String MSG_MAGIC_RUBY_FLAME_SWORD_NEED_GEM = "message.typemoonworld.magic.ruby_flame_sword.need_gem";

    public static final String KEY_MAGIC_SAPPHIRE_WINTER_FROST = "gui.typemoonworld.magic.sapphire_winter_frost";
    public static final String KEY_MAGIC_SAPPHIRE_WINTER_FROST_SELECTED = "gui.typemoonworld.magic.sapphire_winter_frost.selected";
    public static final String KEY_MAGIC_SAPPHIRE_WINTER_FROST_SHORT = "gui.typemoonworld.magic.sapphire_winter_frost.short";
    public static final String MSG_MAGIC_SAPPHIRE_WINTER_FROST_NEED_GEM = "message.typemoonworld.magic.sapphire_winter_frost.need_gem";

    public static final String KEY_MAGIC_EMERALD_WINTER_RIVER = "gui.typemoonworld.magic.emerald_winter_river";
    public static final String KEY_MAGIC_EMERALD_WINTER_RIVER_SELECTED = "gui.typemoonworld.magic.emerald_winter_river.selected";
    public static final String KEY_MAGIC_EMERALD_WINTER_RIVER_SHORT = "gui.typemoonworld.magic.emerald_winter_river.short";
    public static final String MSG_MAGIC_EMERALD_WINTER_RIVER_NEED_GEM = "message.typemoonworld.magic.emerald_winter_river.need_gem";

    public static final String KEY_MAGIC_TOPAZ_REINFORCEMENT = "gui.typemoonworld.magic.topaz_reinforcement";
    public static final String KEY_MAGIC_TOPAZ_REINFORCEMENT_SELECTED = "gui.typemoonworld.magic.topaz_reinforcement.selected";
    public static final String KEY_MAGIC_TOPAZ_REINFORCEMENT_SHORT = "gui.typemoonworld.magic.topaz_reinforcement.short";
    public static final String MSG_MAGIC_TOPAZ_REINFORCEMENT_NEED_GEM = "message.typemoonworld.magic.topaz_reinforcement.need_gem";

    // New Jewel Magic Integration
    public static final String KEY_MAGIC_JEWEL_SHOOT = "key.typemoonworld.magic.jewel_shoot";
    public static final String KEY_MAGIC_JEWEL_SHOOT_SELECTED = "key.typemoonworld.magic.jewel_shoot.selected";
    public static final String KEY_MAGIC_JEWEL_SHOOT_SHORT = "key.typemoonworld.magic.jewel_shoot.short";

    public static final String KEY_MAGIC_JEWEL_RELEASE = "key.typemoonworld.magic.jewel_release";
    public static final String KEY_MAGIC_JEWEL_RELEASE_SELECTED = "key.typemoonworld.magic.jewel_release.selected";
    public static final String KEY_MAGIC_JEWEL_RELEASE_SHORT = "key.typemoonworld.magic.jewel_release.short";
    
    public static final String MSG_MAGIC_JEWEL_MODE_CHANGE = "message.typemoonworld.magic.jewel.mode_change";
    public static final String MSG_MAGIC_ANY_GEM_NEED = "message.typemoonworld.magic.any_gem.need";

    // Projection Magic
    public static final String KEY_MAGIC_PROJECTION = "key.typemoonworld.magic.projection";
    public static final String KEY_MAGIC_PROJECTION_SELECTED = "key.typemoonworld.magic.projection.selected";
    public static final String KEY_MAGIC_PROJECTION_SHORT = "key.typemoonworld.magic.projection.short";

    public static final String KEY_MAGIC_STRUCTURAL_ANALYSIS = "key.typemoonworld.magic.structural_analysis";
    public static final String KEY_MAGIC_STRUCTURAL_ANALYSIS_SELECTED = "key.typemoonworld.magic.structural_analysis.selected";
    public static final String KEY_MAGIC_STRUCTURAL_ANALYSIS_SHORT = "key.typemoonworld.magic.structural_analysis.short";
    
    public static final String KEY_MAGIC_BROKEN_PHANTASM = "key.typemoonworld.magic.broken_phantasm";
    public static final String KEY_MAGIC_BROKEN_PHANTASM_SELECTED = "key.typemoonworld.magic.broken_phantasm.selected";
    public static final String KEY_MAGIC_BROKEN_PHANTASM_SHORT = "key.typemoonworld.magic.broken_phantasm.short";
    
    public static final String KEY_MAGIC_UNLIMITED_BLADE_WORKS = "key.typemoonworld.magic.unlimited_blade_works";
    public static final String KEY_MAGIC_UNLIMITED_BLADE_WORKS_SELECTED = "key.typemoonworld.magic.unlimited_blade_works.selected";
    public static final String KEY_MAGIC_UNLIMITED_BLADE_WORKS_SHORT = "key.typemoonworld.magic.unlimited_blade_works.short";
    
    public static final String KEY_MAGIC_SWORD_BARREL_FULL_OPEN = "key.typemoonworld.magic.sword_barrel_full_open";
    public static final String KEY_MAGIC_SWORD_BARREL_FULL_OPEN_SELECTED = "key.typemoonworld.magic.sword_barrel_full_open.selected";
    public static final String KEY_MAGIC_SWORD_BARREL_FULL_OPEN_SHORT = "key.typemoonworld.magic.sword_barrel_full_open.short";
    public static final String MSG_MAGIC_SWORD_BARREL_MODE_CHANGE = "message.typemoonworld.magic.sword_barrel.mode_change";

    // Reinforcement
    public static final String KEY_MAGIC_REINFORCEMENT = "key.typemoonworld.magic.reinforcement";
    public static final String KEY_MAGIC_REINFORCEMENT_SELECTED = "key.typemoonworld.magic.reinforcement.selected";
    public static final String KEY_MAGIC_REINFORCEMENT_SHORT = "key.typemoonworld.magic.reinforcement.short";

    public static final String KEY_MAGIC_REINFORCEMENT_SELF = "key.typemoonworld.magic.reinforcement_self";
    public static final String KEY_MAGIC_REINFORCEMENT_SELF_SELECTED = "key.typemoonworld.magic.reinforcement_self.selected";
    public static final String KEY_MAGIC_REINFORCEMENT_SELF_SHORT = "key.typemoonworld.magic.reinforcement_self.short";

    public static final String KEY_MAGIC_REINFORCEMENT_OTHER = "key.typemoonworld.magic.reinforcement_other";
    public static final String KEY_MAGIC_REINFORCEMENT_OTHER_SELECTED = "key.typemoonworld.magic.reinforcement_other.selected";
    public static final String KEY_MAGIC_REINFORCEMENT_OTHER_SHORT = "key.typemoonworld.magic.reinforcement_other.short";

    public static final String KEY_MAGIC_REINFORCEMENT_ITEM = "key.typemoonworld.magic.reinforcement_item";
    public static final String KEY_MAGIC_REINFORCEMENT_ITEM_SELECTED = "key.typemoonworld.magic.reinforcement_item.selected";
    public static final String KEY_MAGIC_REINFORCEMENT_ITEM_SHORT = "key.typemoonworld.magic.reinforcement_item.short";

    // General Magic Messages
    public static final String MSG_TRACE_ON = "message.typemoonworld.trace_on";
    public static final String MSG_TRACE_OFF = "message.typemoonworld.trace_off";
    public static final String MSG_NOT_ENOUGH_MANA = "message.typemoonworld.not_enough_mana";
    
    // UBW Messages
    public static final String MSG_UBW_BROKEN_PHANTASM_ON = "message.typemoonworld.ubw.broken_phantasm.on";
    public static final String MSG_UBW_BROKEN_PHANTASM_OFF = "message.typemoonworld.ubw.broken_phantasm.off";
    
    // Structural Analysis Messages
    public static final String MSG_STRUCTURAL_ANALYSIS_NO_TARGET = "message.typemoonworld.structural_analysis.no_target";
    public static final String MSG_STRUCTURAL_ANALYSIS_FAILED = "message.typemoonworld.structural_analysis.failed";
    public static final String MSG_STRUCTURAL_ANALYSIS_CANNOT_ANALYZE_DIVINE = "message.typemoonworld.structural_analysis.cannot_analyze_divine";
    public static final String MSG_PROJECTION_ALREADY_ANALYZED = "message.typemoonworld.projection.already_analyzed";
    public static final String MSG_PROJECTION_CANNOT_ANALYZE_PROJECTED = "message.typemoonworld.projection.cannot_analyze_projected";
    public static final String MSG_PROJECTION_ANALYSIS_COMPLETE = "message.typemoonworld.projection.analysis_complete";
    
    // Projection Messages
    public static final String MSG_PROJECTION_NO_TARGET = "message.typemoonworld.projection.no_target";
    public static final String MSG_PROJECTION_CANNOT_PROJECT_DIVINE = "message.typemoonworld.projection.cannot_project_divine";
    public static final String MSG_PROJECTION_HANDS_FULL = "message.typemoonworld.projection.hands_full";
    public static final String MSG_PROJECTION_TOOLTIP = "message.typemoonworld.projection.tooltip";
    public static final String MSG_PROJECTION_TOOLTIP_INFINITE = "message.typemoonworld.projection.tooltip.infinite";
}
