package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceRank;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantDamageCalculator;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantFaction;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;

public final class ServantCardTraitService {
   private static final String TRAIT_PREFIX = "TypeMoonTrait_";
   private static final String TRAIT_LIST_TAG = "ServantCardTraitKeys";
   private static final String CLASS_TAG = "ServantCardClass";
   private static final String FACTION_TAG = "ServantCardFaction";
   private static final String BATTLE_CONTINUATION_RECOVERY_ACTIVE_TAG = "BattleContinuationRecoveryActive";
   private static final String BATTLE_CONTINUATION_LAST_HEAL_TICK_TAG = "BattleContinuationLastHealTick";
   private static final int BATTLE_CONTINUATION_HEAL_INTERVAL_TICKS = 20;
   private static final float BATTLE_CONTINUATION_HEAL_AMOUNT = 2.0F;

   private ServantCardTraitService() {
   }

   public static void apply(ServerPlayer player, ServantDefinition definition) {
      if (player == null || definition == null) {
         return;
      }

      clear(player);
      CompoundTag data = player.getPersistentData();
      data.putString(CLASS_TAG, definition.classType().key());
      data.putString(FACTION_TAG, definition.faction().key());
      data.putString(TRAIT_LIST_TAG, traitKeyList(definition.traits()));
      for (ServantTraitTag trait : definition.traits()) {
         data.putBoolean(TRAIT_PREFIX + trait.key(), true);
      }
      data.putBoolean("TypeMoonLivingHuman", definition.traits().contains(ServantTraitTag.LIVING_HUMAN));
      data.putBoolean("TypeMoonServantLike", true);

      applyPassiveSkillTags(player, definition);
   }

   public static void tick(ServerPlayer player) {
      if (player == null) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      if (data.getInt("BattleContinuationCooldown") > 0) {
         data.putInt("BattleContinuationCooldown", data.getInt("BattleContinuationCooldown") - 1);
      }
      if (!data.getBoolean(BATTLE_CONTINUATION_RECOVERY_ACTIVE_TAG)) {
         return;
      }
      if (!player.isAlive() || player.getHealth() >= player.getMaxHealth()) {
         data.remove(BATTLE_CONTINUATION_RECOVERY_ACTIVE_TAG);
         data.remove(BATTLE_CONTINUATION_LAST_HEAL_TICK_TAG);
         return;
      }
      long now = player.level().getGameTime();
      long lastHealTick = data.getLong(BATTLE_CONTINUATION_LAST_HEAL_TICK_TAG);
      if (lastHealTick > 0L && now - lastHealTick < BATTLE_CONTINUATION_HEAL_INTERVAL_TICKS) {
         return;
      }
      player.heal(BATTLE_CONTINUATION_HEAL_AMOUNT);
      data.putLong(BATTLE_CONTINUATION_LAST_HEAL_TICK_TAG, now);
   }

   public static void clear(ServerPlayer player) {
      if (player == null) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      data.remove(TRAIT_LIST_TAG);
      data.remove(CLASS_TAG);
      data.remove(FACTION_TAG);
      data.remove("TypeMoonLivingHuman");
      data.remove("TypeMoonServantLike");
      for (ServantTraitTag trait : ServantTraitTag.values()) {
         data.remove(TRAIT_PREFIX + trait.key());
      }
      clearPassiveSkillTags(data);
   }

   public static float applyOutgoingDamage(LivingEntity attacker, LivingEntity defender, float amount) {
      if (!(attacker instanceof Player player) || defender == null || amount <= 0.0F) {
         return amount;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed) {
         return amount;
      }

      ServantDefinition attackerDefinition = ServantIdentityHelper.definitionOf(attacker);
      if (attackerDefinition == null) {
         return amount;
      }

      double multiplier = 1.0;
      multiplier *= ServantDamageCalculator.computeFactionMultiplier(attackerDefinition.faction(), ServantIdentityHelper.factionOf(defender));
      multiplier *= ServantDamageCalculator.computeTraitMultiplier(
         specialAttackConditions(attackerDefinition),
         ServantIdentityHelper.traitsOf(defender)
      );

      CompoundTag data = attacker.getPersistentData();
      if (data.getBoolean("MadEnhancementActive")) {
         multiplier *= 1.0 + Math.max(0.0F, data.getFloat("MadEnhancementDamageBonus"));
      }
      if (data.getBoolean("IndependentActionActive") && attacker.getRandom().nextFloat() < 0.18F) {
         multiplier *= 1.0 + Math.max(0.0F, data.getFloat("IndependentActionCritDamageBonus"));
      }
      if (data.getBoolean("CritDamageBoostActive") && attacker.getRandom().nextFloat() < 0.16F) {
         multiplier *= Math.max(1.0F, data.getFloat("CritDamageMultiplier"));
      }
      if (data.getBoolean("ClairvoyanceActive") && attacker.getRandom().nextFloat() < 0.12F) {
         multiplier *= 1.0 + Math.max(0.0F, data.getFloat("ClairvoyanceCritBonus"));
      }

      float result = (float)Math.max(0.0, amount * multiplier);
      if (data.getBoolean("DivinityActive")) {
         result += Math.max(0.0F, data.getFloat("DivinityFlatDamage"));
      }
      return result;
   }

   private static void applyPassiveSkillTags(ServerPlayer player, ServantDefinition definition) {
      CompoundTag data = player.getPersistentData();
      MagicResistanceRank magicResistance = magicResistanceRank(definition.skillIds());
      if (magicResistance != MagicResistanceRank.NONE) {
         MagicResistanceHelper.setMagicResistance(
            player,
            magicResistance,
            MagicResistanceHelper.damageReductionForRank(magicResistance),
            debuffResistanceForRank(magicResistance)
         );
      }

      if (hasSkill(definition, "battle_continuation_a")) {
         data.putBoolean("BattleContinuationActive", true);
         if (!data.contains("BattleContinuationCooldown")) {
            data.putInt("BattleContinuationCooldown", 0);
         }
         data.putInt("BattleContinuationMaxCooldown", 6000);
      }
      if (hasSkill(definition, "emiya_style_battle_continuation")) {
         data.putBoolean("EmiyaStyleBattleContinuationActive", true);
      }
      if (hasSkill(definition, "artoria_instinct_a")) {
         data.putBoolean("ArtoriaInstinctAActive", true);
         data.putFloat("ArtoriaInstinctCertainHitNegationChance", 0.95F);
      }
      if (hasSkill(definition, "mana_burst_a")) {
         data.putBoolean("ArtoriaManaBurstAAvailable", true);
      }
      if (hasSkill(definition, "charisma_b")) {
         data.putBoolean("ArtoriaCharismaBAvailable", true);
      }
      if (hasSkillPrefix(definition, "divinity_") || definition.traits().contains(ServantTraitTag.DIVINE)) {
         data.putBoolean("DivinityActive", true);
         data.putFloat("DivinityFlatDamage", divinityFlatDamage(definition));
      }
      if (hasSkill(definition, "mad_enhancement_b")) {
         data.putBoolean("MadEnhancementActive", true);
         data.putFloat("MadEnhancementPenalty", 0.3F);
         data.putFloat("MadEnhancementDamageBonus", 0.15F);
      }
      if (hasSkill(definition, "false_mind_eye_b")) {
         data.putBoolean("CritDamageBoostActive", true);
         data.putFloat("CritDamageMultiplier", 2.0F);
      }
      if (hasSkill(definition, "true_mind_eye_b")) {
         data.putBoolean(SasakiKojiroCombatHelper.MINDSEYE_ACTIVE_TAG, true);
         data.putFloat(SasakiKojiroCombatHelper.MINDSEYE_DODGE_CHANCE_TAG, 0.55F);
         data.putFloat(SasakiKojiroCombatHelper.MINDSEYE_BLOCK_CHANCE_TAG, 0.35F);
      }
      if (hasSkill(definition, "projection_magic_c")) {
         data.putBoolean("ProjectionMagicActive", true);
         data.putFloat("ProjectionMagicSwordDiscount", 0.5F);
         data.putInt("ProjectionMagicLifetimeTicks", 100);
      }
      if (hasSkill(definition, "clairvoyance_c")) {
         data.putBoolean("ClairvoyanceActive", true);
         data.putFloat("ClairvoyanceAccuracyBonus", 0.30F);
         data.putFloat("ClairvoyanceCritBonus", 0.10F);
      }
      if (hasSkill(definition, "independent_action_a")) {
         data.putBoolean("IndependentActionActive", true);
         data.putFloat("IndependentActionCritDamageBonus", 0.12F);
      } else if (hasSkill(definition, "independent_action_b")) {
         data.putBoolean("IndependentActionActive", true);
         data.putFloat("IndependentActionCritDamageBonus", 0.08F);
      } else if (hasSkill(definition, "independent_action_c")) {
         data.putBoolean("IndependentActionActive", true);
         data.putFloat("IndependentActionCritDamageBonus", 0.05F);
      }
      if (hasSkill(definition, "riding_a_plus")) {
         data.putBoolean("RidingAPlusActive", true);
         data.putFloat("RidingAPlusSpeedBonus", 0.5F);
         data.putFloat("RidingAPlusArmorBonus", 0.2F);
      } else if (hasSkill(definition, "riding_b")) {
         data.putBoolean("RidingBActive", true);
         data.putFloat("RidingBSpeedBonus", 0.2F);
         data.putFloat("RidingBArmorBonus", 0.1F);
      }
   }

   private static void clearPassiveSkillTags(CompoundTag data) {
      data.remove(MagicResistanceHelper.MAGIC_RESISTANCE_LEVEL_TAG);
      data.remove(MagicResistanceHelper.MAGIC_RESISTANCE_DAMAGE_REDUCTION_TAG);
      data.remove(MagicResistanceHelper.MAGIC_RESISTANCE_DEBUFF_RESIST_TAG);
      data.remove("BattleContinuationActive");
      data.remove("BattleContinuationCooldown");
      data.remove("BattleContinuationMaxCooldown");
      data.remove("BattleContinuationRecoveryActive");
      data.remove("BattleContinuationLastHealTick");
      data.remove("EmiyaStyleBattleContinuationActive");
      data.remove("ArtoriaInstinctAActive");
      data.remove("ArtoriaInstinctCertainHitNegationChance");
      data.remove("ArtoriaManaBurstAAvailable");
      data.remove("ArtoriaCharismaBAvailable");
      data.remove("DivinityActive");
      data.remove("DivinityFlatDamage");
      data.remove("MadEnhancementActive");
      data.remove("MadEnhancementPenalty");
      data.remove("MadEnhancementDamageBonus");
      data.remove("CritDamageBoostActive");
      data.remove("CritDamageMultiplier");
      data.remove("CritDamageTicksRemaining");
      data.remove(SasakiKojiroCombatHelper.MINDSEYE_ACTIVE_TAG);
      data.remove(SasakiKojiroCombatHelper.MINDSEYE_DODGE_CHANCE_TAG);
      data.remove(SasakiKojiroCombatHelper.MINDSEYE_BLOCK_CHANCE_TAG);
      data.remove("ProjectionMagicActive");
      data.remove("ProjectionMagicSwordDiscount");
      data.remove("ProjectionMagicLifetimeTicks");
      data.remove("ClairvoyanceActive");
      data.remove("ClairvoyanceAccuracyBonus");
      data.remove("ClairvoyanceCritBonus");
      data.remove("IndependentActionActive");
      data.remove("IndependentActionCritDamageBonus");
      data.remove("RidingAPlusActive");
      data.remove("RidingAPlusSpeedBonus");
      data.remove("RidingAPlusArmorBonus");
      data.remove("RidingBActive");
      data.remove("RidingBSpeedBonus");
      data.remove("RidingBArmorBonus");
   }

   private static List<ServantTraitTag> specialAttackConditions(ServantDefinition definition) {
      EnumSet<ServantTraitTag> conditions = EnumSet.noneOf(ServantTraitTag.class);
      switch (definition.id()) {
         case "oda_nobunaga" -> {
            conditions.add(ServantTraitTag.DIVINE);
            conditions.add(ServantTraitTag.DEMONIC);
            conditions.add(ServantTraitTag.FAIRY_TALE);
            conditions.add(ServantTraitTag.MOUNTED);
            conditions.add(ServantTraitTag.RIDING);
         }
         case "enkidu" -> {
            conditions.add(ServantTraitTag.DIVINE);
            conditions.add(ServantTraitTag.CELESTIAL);
         }
         case "medusa" -> conditions.add(ServantTraitTag.MALE);
         case "artoria_pendragon", "gawain" -> conditions.add(ServantTraitTag.DRAGON);
         case "cursed_arm_hassan", "li_shuwen", "sasaki_kojiro" -> {
            conditions.add(ServantTraitTag.HUMANOID);
            conditions.add(ServantTraitTag.LIVING_HUMAN);
         }
         default -> {
         }
      }
      return new ArrayList<>(conditions);
   }

   private static String traitKeyList(List<ServantTraitTag> traits) {
      StringBuilder builder = new StringBuilder();
      for (ServantTraitTag trait : traits) {
         if (builder.length() > 0) {
            builder.append(',');
         }
         builder.append(trait.key());
      }
      return builder.toString();
   }

   private static boolean hasSkill(ServantDefinition definition, String skillId) {
      return definition.skillIds() != null && definition.skillIds().contains(skillId);
   }

   private static boolean hasSkillPrefix(ServantDefinition definition, String prefix) {
      if (definition.skillIds() == null) {
         return false;
      }
      for (String skill : definition.skillIds()) {
         if (skill != null && skill.startsWith(prefix)) {
            return true;
         }
      }
      return false;
   }

   private static MagicResistanceRank magicResistanceRank(List<String> skillIds) {
      MagicResistanceRank best = MagicResistanceRank.NONE;
      if (skillIds == null) {
         return best;
      }
      for (String skill : skillIds) {
         MagicResistanceRank rank = magicResistanceRank(skill);
         if (rank.isAtLeast(best)) {
            best = rank;
         }
      }
      return best;
   }

   private static MagicResistanceRank magicResistanceRank(String skillId) {
      if (skillId == null || !skillId.startsWith("magic_resistance_")) {
         return MagicResistanceRank.NONE;
      }
      String rank = skillId.substring("magic_resistance_".length());
      if (rank.startsWith("a")) {
         return MagicResistanceRank.A;
      }
      if (rank.startsWith("b")) {
         return MagicResistanceRank.B;
      }
      if (rank.startsWith("c")) {
         return MagicResistanceRank.C;
      }
      if (rank.startsWith("d")) {
         return MagicResistanceRank.D;
      }
      if (rank.startsWith("e")) {
         return MagicResistanceRank.E;
      }
      return MagicResistanceRank.NONE;
   }

   private static float debuffResistanceForRank(MagicResistanceRank rank) {
      return switch (rank == null ? MagicResistanceRank.NONE : rank) {
         case A -> 0.25F;
         case B -> 0.175F;
         case C -> 0.10F;
         default -> 0.0F;
      };
   }

   private static float divinityFlatDamage(ServantDefinition definition) {
      if (hasSkill(definition, "divinity_a") || hasSkill(definition, "god_hand_passive")) {
         return 25.0F;
      }
      if (hasSkill(definition, "divinity_b_plus")) {
         return 12.0F;
      }
      if (hasSkill(definition, "divinity_b")) {
         return 8.0F;
      }
      if (hasSkill(definition, "divinity_c")) {
         return 5.0F;
      }
      if (hasSkill(definition, "divinity_d")) {
         return 3.0F;
      }
      if (hasSkill(definition, "divinity_e") || hasSkill(definition, "divinity_e_minus")) {
         return 1.0F;
      }
      return definition.traits().contains(ServantTraitTag.DIVINE) ? 3.0F : 0.0F;
   }
}
