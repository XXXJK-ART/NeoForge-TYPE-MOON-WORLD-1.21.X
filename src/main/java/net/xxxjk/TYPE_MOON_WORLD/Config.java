package net.xxxjk.TYPE_MOON_WORLD;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// 一个示例配置类，这不是必需的，但最好有一个，以便让你的配置保持条理。

// Demonstrates how to use Neo's config APIs
// 演示如何使用 Neo 的配置 API。
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
@SuppressWarnings("null")
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    private static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    public static final ModConfigSpec.BooleanValue GEM_RESONANCE_ENABLED = BUILDER
            .comment("Enable gem resonance cycle in gem terrain")
            .define("gemResonanceEnabled", true);

    public static final ModConfigSpec.IntValue GEM_RESONANCE_CYCLE_TICKS = BUILDER
            .comment("Gem resonance cycle length in ticks")
            .defineInRange("gemResonanceCycleTicks", 9600, 200, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue GEM_RESONANCE_DURATION_TICKS = BUILDER
            .comment("Gem resonance active duration in ticks")
            .defineInRange("gemResonanceDurationTicks", 900, 20, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue GEM_BONUS_DROP_CHANCE = BUILDER
            .comment("Extra raw gem drop chance during resonance")
            .defineInRange("gemBonusDropChance", 0.15D, 0.0D, 1.0D);

    public static final ModConfigSpec.BooleanValue GEM_MIST_ENABLED = BUILDER
            .comment("Enable crystal mist ambient pulses in gem terrain")
            .define("gemMistEnabled", true);

    // a list of strings that are treated as resource locations for items
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineList("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;
    public static boolean gemResonanceEnabled;
    public static int gemResonanceCycleTicks;
    public static int gemResonanceDurationTicks;
    public static double gemBonusDropChance;
    public static boolean gemMistEnabled;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();
        gemResonanceEnabled = GEM_RESONANCE_ENABLED.get();
        gemResonanceCycleTicks = GEM_RESONANCE_CYCLE_TICKS.get();
        gemResonanceDurationTicks = Math.min(gemResonanceCycleTicks, GEM_RESONANCE_DURATION_TICKS.get());
        gemBonusDropChance = GEM_BONUS_DROP_CHANCE.get();
        gemMistEnabled = GEM_MIST_ENABLED.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream()
                .map(itemName -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemName)))
                .collect(Collectors.toSet());
    }
}
