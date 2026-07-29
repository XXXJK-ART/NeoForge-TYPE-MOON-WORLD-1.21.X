package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatFormulas;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantSkillDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantClassType;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.CombatFact;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactCondition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactType;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactBypass;

/** Resolves definition, skill, action and live-state information into bounded AI facts. */
public final class ServantCapabilityResolver {
   private static final Map<String, CachedFacts> CACHE = new ConcurrentHashMap<>();
   private static final Map<LivingEntity, LiveSnapshots> LIVE_CACHE = new WeakHashMap<>();

   private ServantCapabilityResolver() { }

   public static CombatCapabilitySnapshot resolve(LivingEntity entity) {
      ServantDefinition definition = ServantIdentityHelper.definitionOf(entity);
      if (entity == null || definition == null) return CombatCapabilitySnapshot.EMPTY;
      long revision = ServantSkillDataRegistry.revision();
      CachedFacts cached = CACHE.get(definition.id());
      if (cached == null || cached.revision != revision) {
         cached = new CachedFacts(revision, collect(definition), collectVisible(definition));
         CACHE.put(definition.id(), cached);
      }
      return liveSnapshots(entity, cached, revision).full;
   }

   public static CombatCapabilitySnapshot resolveVisible(LivingEntity entity) {
      ServantDefinition definition = ServantIdentityHelper.definitionOf(entity);
      if (entity == null || definition == null) return CombatCapabilitySnapshot.EMPTY;
      long revision = ServantSkillDataRegistry.revision();
      CachedFacts cached = CACHE.get(definition.id());
      if (cached == null || cached.revision != revision) {
         cached = new CachedFacts(revision, collect(definition), collectVisible(definition));
         CACHE.put(definition.id(), cached);
      }
      return liveSnapshots(entity, cached, revision).visible;
   }

   private static LiveSnapshots liveSnapshots(LivingEntity entity, CachedFacts facts, long revision) {
      long tick = entity.level().getGameTime();
      synchronized (LIVE_CACHE) {
         LiveSnapshots current = LIVE_CACHE.get(entity);
         if (current != null && current.tick == tick && current.revision == revision) return current;
         LiveSnapshots resolved = new LiveSnapshots(tick, revision,
            resolveActive(entity, facts.facts), resolveActive(entity, facts.visibleFacts));
         LIVE_CACHE.put(entity, resolved);
         return resolved;
      }
   }

   private static CombatCapabilitySnapshot resolveActive(LivingEntity entity, List<ResolvedFact> facts) {
      EnumMap<FactType, Double> active = new EnumMap<>(FactType.class);
      EnumMap<FactType, Set<FactBypass>> bypasses = new EnumMap<>(FactType.class);
      for (ResolvedFact fact : facts) {
         if (fact.type == FactType.UNKNOWN || !conditionsMet(entity, fact.conditions)) continue;
         active.merge(fact.type, fact.strength, Math::max);
         if (!fact.bypasses.isEmpty()) {
            EnumSet<FactBypass> merged = EnumSet.noneOf(FactBypass.class);
            merged.addAll(bypasses.getOrDefault(fact.type, Set.of()));
            merged.addAll(fact.bypasses);
            bypasses.put(fact.type, Set.copyOf(merged));
         }
      }
      return new CombatCapabilitySnapshot(active, bypasses);
   }

   public static boolean isMobile(LivingEntity entity) {
      return entity != null && entity.isAlive()
         && !entity.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
         && !entity.hasEffect(ModMobEffects.BINDING)
         && !entity.hasEffect(ModMobEffects.PETRIFIED)
         && !entity.hasEffect(ModMobEffects.STAGGER)
         && entity.getTicksFrozen() <= 0
         && !ServantCombatSystem.cannotAct(entity)
         && !EnkiduCombatHelper.isBoundByChainsOfHeaven(entity)
         && !entity.getPersistentData().getBoolean("TypeMoonMovementBound")
         && (!(entity instanceof Mob mob) || !mob.getNavigation().isStuck());
   }

   private static List<ResolvedFact> collect(ServantDefinition definition) {
      java.util.ArrayList<ResolvedFact> result = new java.util.ArrayList<>(collectVisible(definition));
      for (String skillId : definition.skillIds()) {
         ServantSkillDefinition skill = ServantSkillDataRegistry.get(skillId);
         if (skill != null) {
            for (CombatFact fact : skill.ai().facts()) {
               result.add(new ResolvedFact(fact.type(), fact.strength(), fact.conditions(), fact.bypassedBy()));
            }
            if (skill.ai().facts().isEmpty()) {
               skill.effects().forEach(effect -> addDerivedEffect(result, effect.effectType()));
               addDerivedSkillId(result, skillId);
            }
         } else {
            addDerivedSkillId(result, skillId);
         }
      }
      return List.copyOf(result);
   }

   private static List<ResolvedFact> collectVisible(ServantDefinition definition) {
      java.util.ArrayList<ResolvedFact> result = new java.util.ArrayList<>();
      addClassFacts(result, definition.classType());
      int agility = ServantCombatFormulas.agilityStep(definition.parameters());
      if (agility >= 4) {
         add(result, FactType.GAP_CLOSE, 0.55 + (agility - 4) * 0.15);
         add(result, FactType.PURSUIT, 0.55 + (agility - 4) * 0.15);
      }
      ServantActionProfile actions = ServantActionRegistry.get(definition.id());
      if (actions != null) {
         for (AiActionDescriptor action : actions.actions()) addActionFacts(result, action);
      }
      return List.copyOf(result);
   }

   private static void addClassFacts(List<ResolvedFact> facts, ServantClassType type) {
      if (type == null) return;
      switch (type) {
         case ARCHER -> add(facts, FactType.PROJECTILE_PRESSURE, 0.7);
         case CASTER -> add(facts, FactType.MAGIC_PRESSURE, 0.7);
         case SABER, LANCER, BERSERKER -> add(facts, FactType.MELEE_PRESSURE, 0.7);
         case RIDER, ASSASSIN -> {
            add(facts, FactType.MELEE_PRESSURE, 0.5);
            add(facts, FactType.GAP_CLOSE, 0.5);
         }
         default -> { }
      }
   }

   private static void addActionFacts(List<ResolvedFact> facts, AiActionDescriptor action) {
      if (action.tags().contains(AiActionDescriptor.Tag.MELEE)) add(facts, FactType.MELEE_PRESSURE, 0.55);
      if (action.tags().contains(AiActionDescriptor.Tag.PROJECTILE)) add(facts, FactType.PROJECTILE_PRESSURE, 0.65);
      if (action.tags().contains(AiActionDescriptor.Tag.AREA)) add(facts, FactType.AREA_CONTROL, 0.6);
      if (action.tags().contains(AiActionDescriptor.Tag.CONTROL)) add(facts, FactType.CONTROL, 0.65);
      if (action.tags().contains(AiActionDescriptor.Tag.HEAL)) add(facts, FactType.HEAL, 0.7);
      if (action.tags().contains(AiActionDescriptor.Tag.PURSUIT)
         || action.tags().contains(AiActionDescriptor.Tag.INTERCEPT)) add(facts, FactType.PURSUIT, 0.65);
      if (action.tags().contains(AiActionDescriptor.Tag.GAP_CLOSER)) add(facts, FactType.GAP_CLOSE, 0.7);
   }

   private static void addDerivedEffect(List<ResolvedFact> facts, String effectType) {
      String key = normalize(effectType);
      if (key.contains("magic_resistance")) add(facts, FactType.MAGIC_RESISTANCE, 0.7);
      else if (key.contains("revive") || key.contains("survival_trigger")) add(facts, FactType.REVIVE, 0.8);
      else if (key.contains("adaptive_defense")) add(facts, FactType.ADAPTIVE_DEFENSE, 0.8);
      else if (key.contains("damage_immunity_threshold")) add(facts, FactType.DAMAGE_THRESHOLD, 0.8);
      else if (key.contains("regeneration") || key.contains("heal")) add(facts, FactType.REGENERATION, 0.6);
      else if (key.contains("shield") || key.contains("damage_resistance")) add(facts, FactType.SHIELD, 0.55);
      else if (key.contains("judgement") || key.contains("prediction")) add(facts, FactType.PREDICTION, 0.75);
      else if (key.contains("stealth") || key.contains("concealment")) add(facts, FactType.CONCEALMENT, 0.7);
      else if (key.contains("evasion") || key.contains("dodge")) add(facts, FactType.PREDICTION, 0.6);
      else if (key.contains("binding") || key.contains("control") || key.contains("slow")) add(facts, FactType.CONTROL, 0.6);
      else if (key.contains("field") || key.contains("infection")) add(facts, FactType.AREA_CONTROL, 0.6);
      else if (key.contains("mounted") || key.contains("mobility")) {
         add(facts, FactType.GAP_CLOSE, 0.6);
         add(facts, FactType.PURSUIT, 0.55);
      }
   }

   private static void addDerivedSkillId(List<ResolvedFact> facts, String skillId) {
      String key = normalize(skillId);
      if (key.contains("protection_from_arrows")) {
         facts.add(new ResolvedFact(FactType.PROJECTILE_NEGATION, 1.0, List.of(FactCondition.MOBILE),
            List.of(FactBypass.IMMOBILIZE, FactBypass.STUN, FactBypass.FREEZE, FactBypass.EXPLOSION,
               FactBypass.PIERCING, FactBypass.SURE_HIT)));
      }
      if (key.contains("mind_eye") || key.contains("mindseye") || key.contains("instinct")
         || key.contains("clairvoyance") || key.contains("seasoned")) add(facts, FactType.PREDICTION, 0.65);
      if (key.contains("presence_concealment") || key.startsWith("stealth_")) add(facts, FactType.CONCEALMENT, 0.75);
      if (key.contains("battle_continuation") || key.contains("belt_of_berhillak")) add(facts, FactType.REVIVE, 0.65);
      if (key.contains("god_hand")) {
         add(facts, FactType.REVIVE, 1.0);
         add(facts, FactType.DAMAGE_THRESHOLD, 1.0);
         add(facts, FactType.ADAPTIVE_DEFENSE, 1.0);
      }
      if (key.contains("magic_resistance")) add(facts, FactType.MAGIC_RESISTANCE, 0.65);
      if (key.contains("nursing") || key.contains("heal") || key.contains("berkana")) add(facts, FactType.HEAL, 0.65);
      if (key.contains("riding") || key.contains("pursuit") || key.contains("step") || key.contains("wandering")) {
         add(facts, FactType.GAP_CLOSE, 0.6);
      }
      if (key.contains("tenka_fubu") || key.contains("anti_mystery")) add(facts, FactType.ANTI_MYSTERY, 0.9);
      if (containsAny(key, "arrow_construction", "projection_magic", "floating_matchlock", "matchlock_volley",
         "fire_barrage", "scorched_banner")) add(facts, FactType.PROJECTILE_PRESSURE, 0.7);
      if (containsAny(key, "clairvoyance", "presence_detection", "strategy", "jizaiten_eye",
         "swallow_technique", "seasoned")) add(facts, FactType.PREDICTION, 0.7);
      if (containsAny(key, "recast_stance", "underworld_guidance")) add(facts, FactType.REGENERATION, 0.7);
      if (containsAny(key, "sunlit_pursuit", "atsumori_step", "dan_no_ura", "usumidori_heaven_blade",
         "riding_ex")) {
         add(facts, FactType.GAP_CLOSE, 0.7);
         add(facts, FactType.PURSUIT, 0.7);
      }
      if (containsAny(key, "flame_tornado", "radiant_field", "ash_field", "demon_king_pressure",
         "infection", "innocent_world", "earth_magic", "fire_magic", "water_magic", "wind_magic")) {
         add(facts, FactType.AREA_CONTROL, 0.7);
      }
      if (containsAny(key, "noon_guard", "benkei_immovable", "philosopher_stone", "perfect_form", "stout")) {
         add(facts, FactType.SHIELD, 0.7);
      }
      if (containsAny(key, "elemental_spirit", "high_speed_chanting", "earth_magic", "fire_magic",
         "water_magic", "wind_magic")) add(facts, FactType.MAGIC_PRESSURE, 0.7);
      if (containsAny(key, "hasebe_thrust", "hasebe_repel", "scorched_crosscut", "close_counter",
         "three_line_rotation", "gallatin_spark", "solar_rebuke", "houmaru_spider_slayer")) {
         add(facts, FactType.MELEE_PRESSURE, 0.7);
      }
      if (containsAny(key, "close_counter", "hasebe_repel", "benkei_immovable")) {
         add(facts, FactType.CONTROL, 0.6);
      }
   }

   private static boolean conditionsMet(LivingEntity entity, List<FactCondition> conditions) {
      for (FactCondition condition : conditions) {
         boolean met = switch (condition) {
            case UNKNOWN -> true;
            case MOBILE -> isMobile(entity);
            case NOT_SILENCED -> !ServantCombatSystem.skillsSuppressed(entity);
            case LOW_HEALTH -> entity.getHealth() <= entity.getMaxHealth() * 0.35F;
            case DAYLIGHT -> entity.level().isDay() && entity.level().canSeeSky(BlockPos.containing(entity.getEyePosition()));
            case ON_GROUND -> entity.onGround();
            case HAS_MANA -> entity instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity servant
               ? servant.getCurrentMp() > 0.0
               : !(entity instanceof net.minecraft.world.entity.player.Player player)
                  || player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES)
                     .servant_card_mana > 0.0;
         };
         if (!met) return false;
      }
      return true;
   }

   private static void add(List<ResolvedFact> facts, FactType type, double strength) {
      facts.add(new ResolvedFact(type, Math.max(0.0, Math.min(1.0, strength)), List.of(), List.of()));
   }

   private static String normalize(String value) {
      return value == null ? "" : value.toLowerCase(Locale.ROOT);
   }

   private static boolean containsAny(String value, String... fragments) {
      for (String fragment : fragments) if (value.contains(fragment)) return true;
      return false;
   }

   private record CachedFacts(long revision, List<ResolvedFact> facts, List<ResolvedFact> visibleFacts) { }
   private record LiveSnapshots(long tick, long revision, CombatCapabilitySnapshot full,
                                CombatCapabilitySnapshot visible) { }
   private record ResolvedFact(FactType type, double strength, List<FactCondition> conditions,
                               List<FactBypass> bypasses) { }
}
