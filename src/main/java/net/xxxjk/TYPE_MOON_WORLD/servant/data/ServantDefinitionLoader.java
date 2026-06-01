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
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSpecialization;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.CombatDisposition;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.MoralAxis;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.ObedienceAxis;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.PrincipleAxis;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.SpecialTargetPrinciple;
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
               ServantDefinition definition = parseDefinitionSafe(id.toString(), element.getAsJsonObject());
               if (definition != null) {
                  definitions.put(definition.id(), definition);
               }
            }
         } catch (Exception e) {
            net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.error("Failed to load servant definition: {}", id, e);
         }
      }

      ServantDataRegistry.reload(definitions);
      net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.info("Loaded {} servant definitions: {}", definitions.size(), definitions.keySet());
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
      ServantSpecialization specialization = parseSpecialization(json);

      String modelGeometry = "";
      String texture = "";
      String animation = "";
      ServantAnimations animations = ServantAnimations.empty();
      if (json.has("model")) {
         JsonObject modelJson = json.getAsJsonObject("model");
         modelGeometry = getStringOrDefault(modelJson, "geometry", "");
         texture = getStringOrDefault(modelJson, "texture", "");
         animation = getStringOrDefault(modelJson, "animation", "");
         animations = parseAnimations(modelJson, animation);
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
      MoralAxis morality = MoralAxis.fromTraits(traits);
      SocialDisposition social = SocialDisposition.NORMAL;
      CombatDisposition combat = CombatDisposition.BALANCED;
      java.util.List<SpecialTargetPrinciple> specialPrinciples = new java.util.ArrayList<>();
      double startingFavor = 50.0;
      if (json.has("personality")) {
         JsonObject personalityJson = json.getAsJsonObject("personality");
         obedience = ObedienceAxis.fromKey(getStringOrDefault(personalityJson, "obedience", "neutral"));
         principle = PrincipleAxis.fromKey(getStringOrDefault(personalityJson, "principle", "neutral"));
         morality = MoralAxis.fromKey(getStringOrDefault(personalityJson, "morality", morality.key()));
         social = SocialDisposition.fromKey(getStringOrDefault(personalityJson, "social", "normal"));
         combat = CombatDisposition.fromKey(getStringOrDefault(personalityJson, "combat", "balanced"));
         if (personalityJson.has("special_principles") && personalityJson.get("special_principles").isJsonArray()) {
            JsonArray principlesArray = personalityJson.getAsJsonArray("special_principles");
            for (JsonElement principleElement : principlesArray) {
               SpecialTargetPrinciple principleValue = SpecialTargetPrinciple.fromKey(principleElement.getAsString());
               if (principleValue != null && !specialPrinciples.contains(principleValue)) {
                  specialPrinciples.add(principleValue);
               }
            }
         }
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
         animations,
         specialization,
         modelGeometry, texture, animation,
         skillIds, noblePhantasmId,
         obedience, principle, morality, social, combat, specialPrinciples,
         startingFavor, aiConfigId,
         primaryColor, secondaryColor
      );
   }

   @Nullable
   private ServantDefinition parseDefinitionSafe(String fallbackId, JsonObject json) {
      try {
         return parseDefinition(fallbackId, json);
      } catch (Exception e) {
         net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.error("Failed to parse definition for {}: {}", fallbackId, e.getMessage(), e);
         return null;
      }
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

   private ServantAnimations parseAnimations(JsonObject modelJson, String legacyAnimationPath) {
      if (modelJson == null || !modelJson.has("animations") || !modelJson.get("animations").isJsonObject()) {
         return ServantAnimations.legacy(legacyAnimationPath);
      }

      JsonObject animationsJson = modelJson.getAsJsonObject("animations");
      String idle = getStringOrDefault(animationsJson, "idle", "");
      String walk = getStringOrDefault(animationsJson, "walk", "");
      java.util.Map<String, String> actions = new java.util.LinkedHashMap<>();
      for (String key : new String[]{"roar", "slam", "jump_attack", "charge", "sweep", "slash", "teleport_behind", "stomp", "uppercut", "horizontal_swing", "tsurigameshi", "gae_bolg_throw", "rune_cast", "fly", "bend_over", "remove_blindfold"}) {
         if (animationsJson.has(key)) {
            String value = animationsJson.get(key).getAsString();
            if (value != null && !value.isBlank()) {
               actions.put(key, value);
            }
         }
      }

      if ((idle == null || idle.isBlank()) && (walk == null || walk.isBlank()) && actions.isEmpty()) {
         return ServantAnimations.legacy(legacyAnimationPath);
      }

      if (idle == null || idle.isBlank()) {
         idle = ServantAnimations.basePrefix(legacyAnimationPath) + ".idle";
      }
      if (walk == null || walk.isBlank()) {
         walk = idle;
      }
      return new ServantAnimations(idle, walk, actions);
   }

   private ServantSpecialization parseSpecialization(JsonObject json) {
      if (json == null || !json.has("specialization") || !json.get("specialization").isJsonObject()) {
         return ServantSpecialization.empty();
      }

      JsonObject spec = json.getAsJsonObject("specialization");
      float bodyWidth = 0.0F;
      float bodyHeight = 0.0F;
      float eyeHeight = 0.0F;
      double handOffsetX = 0.0;
      double handOffsetY = 0.0;
      double handOffsetZ = 0.0;
      if (spec.has("dimensions") && spec.get("dimensions").isJsonObject()) {
         JsonObject dimensions = spec.getAsJsonObject("dimensions");
         bodyWidth = (float)getDoubleOrDefault(dimensions, "width", 0.0);
         bodyHeight = (float)getDoubleOrDefault(dimensions, "height", 0.0);
         eyeHeight = (float)getDoubleOrDefault(dimensions, "eye_height", 0.0);
      }
      if (spec.has("hand_item_offset") && spec.get("hand_item_offset").isJsonObject()) {
         JsonObject offset = spec.getAsJsonObject("hand_item_offset");
         handOffsetX = getDoubleOrDefault(offset, "x", 0.0);
         handOffsetY = getDoubleOrDefault(offset, "y", 0.0);
         handOffsetZ = getDoubleOrDefault(offset, "z", 0.0);
      }
      String defaultWeapon = getStringOrDefault(spec, "default_weapon", "");
      boolean immuneToStoneAxeDebuff = getBooleanOrDefault(spec, "immune_to_stone_axe_debuff", false);
      java.util.Set<String> combatActions = new java.util.LinkedHashSet<>();
      if (spec.has("combat_actions") && spec.get("combat_actions").isJsonArray()) {
         JsonArray actionsArray = spec.getAsJsonArray("combat_actions");
         for (JsonElement actionElement : actionsArray) {
            String action = actionElement.getAsString();
            if (action != null && !action.isBlank()) {
               combatActions.add(action);
            }
         }
      }
      return ServantSpecialization.of(
         bodyWidth, bodyHeight, eyeHeight,
         handOffsetX, handOffsetY, handOffsetZ,
         defaultWeapon, immuneToStoneAxeDebuff, combatActions
      );
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
