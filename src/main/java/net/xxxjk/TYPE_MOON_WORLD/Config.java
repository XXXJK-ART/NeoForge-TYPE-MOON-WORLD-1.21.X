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
   public static final BooleanValue TERRAIN_DESTRUCTION_ENABLED = BUILDER.comment("Enable terrain impacts produced by TYPE-MOON abilities")
      .define("terrainDestructionEnabled", true);
   public static final BooleanValue NPC_TERRAIN_DESTRUCTION_ENABLED = BUILDER.comment("Allow NPC abilities to damage terrain (also requires mobGriefing)")
      .define("npcTerrainDestructionEnabled", true);
   public static final BooleanValue PLAYER_TERRAIN_DESTRUCTION_ENABLED = BUILDER.comment("Allow player abilities to damage terrain")
      .define("playerTerrainDestructionEnabled", true);
   public static final IntValue TERRAIN_CHECKS_PER_TICK = BUILDER.comment("Maximum queued terrain voxel checks per dimension and tick")
      .defineInRange("terrainChecksPerTick", 8000, 1000, 20000);
   public static final IntValue TERRAIN_BUDGET_MICROS = BUILDER.comment("Soft terrain processing time budget per dimension and tick, in microseconds")
      .defineInRange("terrainBudgetMicros", 4000, 1000, 12000);
   public static final IntValue MAX_QUEUED_TERRAIN_JOBS = BUILDER.comment(
      "Maximum queued terrain destruction jobs per dimension")
      .defineInRange("maxQueuedTerrainJobs", 64, 8, 256);
   public static final IntValue TERRAIN_DEBRIS_QUALITY = BUILDER.comment("Client debris quality: 0=LOW, 1=MEDIUM, 2=HIGH")
      .defineInRange("terrainDebrisQuality", 1, 0, 2);
   public static final BooleanValue PHYSICAL_TERRAIN_DEBRIS_ENABLED = BUILDER.comment(
      "Allow medium and stronger terrain impacts to launch real falling-block entities")
      .define("physicalTerrainDebrisEnabled", true);
   public static final IntValue MAX_PHYSICAL_TERRAIN_DEBRIS = BUILDER.comment(
      "Maximum active TYPE-MOON physical terrain debris entities per dimension")
      .defineInRange("maxPhysicalTerrainDebris", 48, 0, 128);
   public static final DoubleValue COMBAT_SPECTACLE_INTENSITY = BUILDER.comment(
      "Daily servant combat spectacle intensity. Raises non-NP knockback debris, wall tunnels and shallow craters.")
      .defineInRange("combatSpectacleIntensity", 1.35, 0.5, 2.5);
   public static final BooleanValue PROTECT_COMBAT_FOOTING = BUILDER.comment(
      "Keep routine combat terrain impacts from digging out the attacker's own footing.")
      .define("protectCombatFooting", true);
   public static final IntValue MAX_CINEMATIC_CHAIN_IMPACTS = BUILDER.comment(
      "Maximum chained wall impacts for heavy routine combat launches")
      .defineInRange("maxCinematicChainImpacts", 4, 1, 6);
   public static final BooleanValue ARBITRATED_COMBAT_AI_ENABLED = BUILDER.comment("Enable intent arbitration for migrated combat NPCs")
      .define("arbitratedCombatAiEnabled", true);
   public static final BooleanValue ADAPTIVE_PERFORMANCE = BUILDER.comment(
      "Allow deferrable terrain and visual work to yield time under server load")
      .define("adaptivePerformance", true);
   public static final IntValue SERVER_PRESSURE_MSPT = BUILDER.comment(
      "EWMA server tick time at which adaptive background work is reduced")
      .defineInRange("serverPressureMspt", 40, 20, 1000);
   public static final IntValue SERVER_CRITICAL_MSPT = BUILDER.comment(
      "EWMA server tick time at which adaptive background work is heavily reduced")
      .defineInRange("serverCriticalMspt", 50, 25, 2000);
   private static final ConfigValue<List<? extends String>> LEGACY_AI_ENTITY_STRINGS = BUILDER.comment(
      "Entity type ids that must remain on LEGACY AI, for example typemoonworld:artoria_pendragon")
      .defineList("legacyAiEntityTypes", List.of(), Config::validateResourceLocation);
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
   public static boolean terrainDestructionEnabled = true;
   public static boolean npcTerrainDestructionEnabled = true;
   public static boolean playerTerrainDestructionEnabled = true;
   public static int terrainChecksPerTick = 8000;
   public static int terrainBudgetMicros = 4000;
   public static int maxQueuedTerrainJobs = 64;
   public static int terrainDebrisQuality = 1;
   public static boolean physicalTerrainDebrisEnabled = true;
   public static int maxPhysicalTerrainDebris = 48;
   public static double combatSpectacleIntensity = 1.35;
   public static boolean protectCombatFooting = true;
   public static int maxCinematicChainImpacts = 4;
   public static boolean arbitratedCombatAiEnabled = true;
   public static boolean adaptivePerformance = true;
   public static int serverPressureMspt = 40;
   public static int serverCriticalMspt = 50;
   public static Set<ResourceLocation> legacyAiEntityTypes = Set.of();

   private static boolean validateItemName(Object obj) {
      return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
   }

   private static boolean validateResourceLocation(Object obj) {
      if (!(obj instanceof String value)) return false;
      try { ResourceLocation.parse(value); return true; } catch (RuntimeException ignored) { return false; }
   }

   @SubscribeEvent
   static void onLoad(ModConfigEvent event) {
      if (event.getConfig().getSpec() != SPEC) {
         return;
      }

      logDirtBlock = (Boolean)LOG_DIRT_BLOCK.get();
      magicNumber = (Integer)MAGIC_NUMBER.get();
      magicNumberIntroduction = (String)MAGIC_NUMBER_INTRODUCTION.get();
      gemResonanceEnabled = (Boolean)GEM_RESONANCE_ENABLED.get();
      gemResonanceCycleTicks = (Integer)GEM_RESONANCE_CYCLE_TICKS.get();
      gemResonanceDurationTicks = Math.min(gemResonanceCycleTicks, (Integer)GEM_RESONANCE_DURATION_TICKS.get());
      gemBonusDropChance = (Double)GEM_BONUS_DROP_CHANCE.get();
      gemMistEnabled = (Boolean)GEM_MIST_ENABLED.get();
      terrainDestructionEnabled = TERRAIN_DESTRUCTION_ENABLED.get();
      npcTerrainDestructionEnabled = NPC_TERRAIN_DESTRUCTION_ENABLED.get();
      playerTerrainDestructionEnabled = PLAYER_TERRAIN_DESTRUCTION_ENABLED.get();
      terrainChecksPerTick = TERRAIN_CHECKS_PER_TICK.get();
      terrainBudgetMicros = TERRAIN_BUDGET_MICROS.get();
      maxQueuedTerrainJobs = MAX_QUEUED_TERRAIN_JOBS.get();
      terrainDebrisQuality = TERRAIN_DEBRIS_QUALITY.get();
      physicalTerrainDebrisEnabled = PHYSICAL_TERRAIN_DEBRIS_ENABLED.get();
      maxPhysicalTerrainDebris = MAX_PHYSICAL_TERRAIN_DEBRIS.get();
      combatSpectacleIntensity = COMBAT_SPECTACLE_INTENSITY.get();
      protectCombatFooting = PROTECT_COMBAT_FOOTING.get();
      maxCinematicChainImpacts = MAX_CINEMATIC_CHAIN_IMPACTS.get();
      arbitratedCombatAiEnabled = ARBITRATED_COMBAT_AI_ENABLED.get();
      adaptivePerformance = ADAPTIVE_PERFORMANCE.get();
      serverPressureMspt = SERVER_PRESSURE_MSPT.get();
      serverCriticalMspt = Math.max(serverPressureMspt, SERVER_CRITICAL_MSPT.get());
      legacyAiEntityTypes = LEGACY_AI_ENTITY_STRINGS.get().stream().map(ResourceLocation::parse).collect(Collectors.toUnmodifiableSet());
      items = ((List<? extends String>)ITEM_STRINGS.get())
         .stream()
         .map(itemName -> (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemName)))
         .collect(Collectors.toSet());
   }
}
