package net.xxxjk.TYPE_MOON_WORLD;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

@EventBusSubscriber(
   modid = "typemoonworld",
   bus = Bus.MOD
)
public class Config {
   private static final Builder BUILDER = new Builder();
   private static final BooleanValue LOG_DIRT_BLOCK = BUILDER.comment("Whether to log the dirt block on common setup").define("logDirtBlock", true);
   private static final IntValue MAGIC_NUMBER = BUILDER.comment("A magic number").defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);
   public static final ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER.comment("What you want the introduction message to be for the magic number")
      .define("magicNumberIntroduction", "The magic number is... ");
   public static final BooleanValue GEM_RESONANCE_ENABLED = BUILDER.comment("Enable gem resonance cycle in gem terrain").define("gemResonanceEnabled", true);
   public static final IntValue GEM_RESONANCE_CYCLE_TICKS = BUILDER.comment("Gem resonance cycle length in ticks")
      .defineInRange("gemResonanceCycleTicks", 9600, 200, Integer.MAX_VALUE);
   public static final IntValue GEM_RESONANCE_DURATION_TICKS = BUILDER.comment("Gem resonance active duration in ticks")
      .defineInRange("gemResonanceDurationTicks", 900, 20, Integer.MAX_VALUE);
   public static final DoubleValue GEM_BONUS_DROP_CHANCE = BUILDER.comment("Extra raw gem drop chance during resonance")
      .defineInRange("gemBonusDropChance", 0.15, 0.0, 1.0);
   public static final BooleanValue GEM_MIST_ENABLED = BUILDER.comment("Enable crystal mist ambient pulses in gem terrain").define("gemMistEnabled", true);
   private static final ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER.comment("A list of items to log on common setup.")
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

   private static boolean validateItemName(Object obj) {
      return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
   }

   @SubscribeEvent
   static void onLoad(ModConfigEvent event) {
      logDirtBlock = (Boolean)LOG_DIRT_BLOCK.get();
      magicNumber = (Integer)MAGIC_NUMBER.get();
      magicNumberIntroduction = (String)MAGIC_NUMBER_INTRODUCTION.get();
      gemResonanceEnabled = (Boolean)GEM_RESONANCE_ENABLED.get();
      gemResonanceCycleTicks = (Integer)GEM_RESONANCE_CYCLE_TICKS.get();
      gemResonanceDurationTicks = Math.min(gemResonanceCycleTicks, (Integer)GEM_RESONANCE_DURATION_TICKS.get());
      gemBonusDropChance = (Double)GEM_BONUS_DROP_CHANCE.get();
      gemMistEnabled = (Boolean)GEM_MIST_ENABLED.get();
      items = ((List<? extends String>)ITEM_STRINGS.get())
         .stream()
         .map(itemName -> (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemName)))
         .collect(Collectors.toSet());
   }
}
