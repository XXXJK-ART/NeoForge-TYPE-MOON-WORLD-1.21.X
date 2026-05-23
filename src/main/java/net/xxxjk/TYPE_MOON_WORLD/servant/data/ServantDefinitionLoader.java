package net.xxxjk.TYPE_MOON_WORLD.servant.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantClassType;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantFaction;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.CombatDisposition;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.ObedienceAxis;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.PrincipleAxis;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.SocialDisposition;
import org.jetbrains.annotations.Nullable;

public class ServantDefinitionLoader extends SimpleJsonResourceReloadListener {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   public static final String DIRECTORY = "servant/definitions";

   public ServantDefinitionLoader() {
      super(GSON, DIRECTORY);
   }

   @Override
   protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
      Map<String, ServantDefinition> definitions = new HashMap<>();

      for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
         ResourceLocation id = entry.getKey();
         JsonElement element = entry.getValue();

         try {
            if (element.isJsonObject()) {
               ServantDefinition definition = parseDefinition(id.toString(), element.getAsJsonObject());
               if (definition != null) {
                  definitions.put(definition.id(), definition);
               }
            }
         } catch (Exception e) {
            net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.error("Failed to load servant definition: {}", id, e);
         }
      }

      ServantDataRegistry.reload(definitions);
      net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.info("Loaded {} servant definitions", definitions.size());
   }

   @Nullable
   private ServantDefinition parseDefinition(String fallbackId, JsonObject json) {
      String id = json.has("id") ? json.get("id").getAsString() : fallbackId;
      String displayName = json.has("display_name") ? json.get("display_name").getAsString() : id;
      String displayNameZh = json.has("display_name_zh") ? json.get("display_name_zh").getAsString() : displayName;

      ServantClassType classType = ServantClassType.fromKey(getStringOrDefault(json, "class_type", "saber"));
      ServantFaction faction = ServantFaction.fromKey(getStringOrDefault(json, "faction", "human"));

      java.util.List<ServantTraitTag> traits = new java.util.ArrayList<>();
      if (json.has("traits")) {
         JsonArray traitsArray = json.getAsJsonArray("traits");
         for (JsonElement traitElement : traitsArray) {
            traits.add(ServantTraitTag.fromKey(traitElement.getAsString()));
         }
      }

      ServantParams parameters = parseParams(json.getAsJsonObject("parameters"));

      String modelGeometry = "";
      String texture = "";
      String animation = "";
      if (json.has("model")) {
         JsonObject modelJson = json.getAsJsonObject("model");
         modelGeometry = getStringOrDefault(modelJson, "geometry", "");
         texture = getStringOrDefault(modelJson, "texture", "");
         animation = getStringOrDefault(modelJson, "animation", "");
      }

      java.util.List<String> skillIds = new java.util.ArrayList<>();
      if (json.has("skills")) {
         JsonArray skillsArray = json.getAsJsonArray("skills");
         for (JsonElement skillElement : skillsArray) {
            skillIds.add(skillElement.getAsString());
         }
      }

      String noblePhantasmId = getStringOrDefault(json, "noble_phantasm", "");

      ObedienceAxis obedience = ObedienceAxis.COOPERATIVE;
      PrincipleAxis principle = PrincipleAxis.NEUTRAL;
      SocialDisposition social = SocialDisposition.NORMAL;
      CombatDisposition combat = CombatDisposition.BALANCED;
      double startingFavor = 50.0;
      if (json.has("personality")) {
         JsonObject personalityJson = json.getAsJsonObject("personality");
         obedience = ObedienceAxis.fromKey(getStringOrDefault(personalityJson, "obedience", "neutral"));
         principle = PrincipleAxis.fromKey(getStringOrDefault(personalityJson, "principle", "neutral"));
         social = SocialDisposition.fromKey(getStringOrDefault(personalityJson, "social", "normal"));
         combat = CombatDisposition.fromKey(getStringOrDefault(personalityJson, "combat", "balanced"));
         startingFavor = getDoubleOrDefault(personalityJson, "starting_favor", 50.0);
      }

      String aiConfigId = getStringOrDefault(json, "ai_config", "default");

      int primaryColor = 0x999999;
      int secondaryColor = 0x666666;
      if (json.has("spawn_egg")) {
         JsonObject eggJson = json.getAsJsonObject("spawn_egg");
         primaryColor = getIntOrDefault(eggJson, "primary", 0x999999);
         secondaryColor = getIntOrDefault(eggJson, "secondary", 0x666666);
      }

      return new ServantDefinition(
         id, displayName, displayNameZh,
         classType, faction, traits, parameters,
         modelGeometry, texture, animation,
         skillIds, noblePhantasmId,
         obedience, principle, social, combat,
         startingFavor, aiConfigId,
         primaryColor, secondaryColor
      );
   }

   private ServantParams parseParams(JsonObject json) {
      if (json == null) {
         return new ServantParams(
            net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank.E, false,
            net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank.E, false,
            net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank.E, false,
            net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank.E, false,
            net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank.E, false
         );
      }

      String endurance = getStringOrDefault(json, "endurance", "E");
      boolean endurancePlus = getBooleanOrDefault(json, "endurance_plus", false);
      String strength = getStringOrDefault(json, "strength", "E");
      boolean strengthPlus = getBooleanOrDefault(json, "strength_plus", false);
      String agility = getStringOrDefault(json, "agility", "E");
      boolean agilityPlus = getBooleanOrDefault(json, "agility_plus", false);
      String magic = getStringOrDefault(json, "magic", "E");
      boolean magicPlus = getBooleanOrDefault(json, "magic_plus", false);
      String luck = getStringOrDefault(json, "luck", "E");
      boolean luckPlus = getBooleanOrDefault(json, "luck_plus", false);

      return ServantParams.of(endurance, endurancePlus, strength, strengthPlus,
         agility, agilityPlus, magic, magicPlus, luck, luckPlus);
   }

   private String getStringOrDefault(JsonObject json, String key, String defaultValue) {
      return json.has(key) ? json.get(key).getAsString() : defaultValue;
   }

   private double getDoubleOrDefault(JsonObject json, String key, double defaultValue) {
      return json.has(key) ? json.get(key).getAsDouble() : defaultValue;
   }

   private int getIntOrDefault(JsonObject json, String key, int defaultValue) {
      return json.has(key) ? json.get(key).getAsInt() : defaultValue;
   }

   private boolean getBooleanOrDefault(JsonObject json, String key, boolean defaultValue) {
      return json.has(key) && json.get(key).getAsBoolean();
   }
}
