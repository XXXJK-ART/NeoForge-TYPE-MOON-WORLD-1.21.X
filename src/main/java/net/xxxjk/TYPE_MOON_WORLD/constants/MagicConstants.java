package net.xxxjk.TYPE_MOON_WORLD.constants;

public class MagicConstants {
    // Ruby Throw Magic
    public static final float RUBY_EXPLOSION_RADIUS = 5.0F;
    public static final float RUBY_THROW_VELOCITY = 3.0F;
    public static final float RUBY_THROW_INACCURACY = 1.0F;
    public static final int RUBY_LIFETIME_TICKS = 200; // 10 seconds

    // Sapphire Throw Magic
    public static final float SAPPHIRE_THROW_VELOCITY = 3.0F;
    public static final float SAPPHIRE_THROW_INACCURACY = 1.0F;
    public static final int SAPPHIRE_ICE_RADIUS = 3;
    public static final int SAPPHIRE_ICE_DURATION = 100; // 5 seconds (20 ticks * 5)
    public static final int SAPPHIRE_DEBUFF_DURATION = 100; // 5 seconds

    // Emerald Use Magic
    public static final int EMERALD_WALL_DURATION = 160; // 8 seconds (range 8-10s)
    public static final int EMERALD_WALL_WIDTH = 3;
    public static final int EMERALD_WALL_HEIGHT = 3;
    public static final int EMERALD_WALL_DISTANCE = 2;

    // Topaz Throw Magic
    public static final float TOPAZ_THROW_VELOCITY = 3.0F;
    public static final float TOPAZ_THROW_INACCURACY = 1.0F;
    public static final int TOPAZ_DEBUFF_DURATION = 200; // 10 seconds
    public static final double TOPAZ_EFFECT_RADIUS = 5.0;

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

    public static final String GUI_MAGIC_EYES_AND_MODIFICATION = "gui.typemoonworld.screen.magic_eyes_and_modification";
    public static final String GUI_LOAD_MAGIC_EYES = "gui.typemoonworld.screen.load_magic_eyes";
    public static final String GUI_WIP = "gui.typemoonworld.screen.wip";
    public static final String GUI_LEARNED_MAGIC = "gui.typemoonworld.screen.learned_magic";

    // UI Constants
    public static final int UI_COLOR_TEXT = -13408513;
}