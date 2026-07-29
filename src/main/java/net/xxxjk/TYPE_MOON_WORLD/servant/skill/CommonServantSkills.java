package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceRank;

public final class CommonServantSkills {
   private CommonServantSkills() {
   }

   public static void registerBuiltin(ServantSkillRegistry registry) {
      registry.register("magic_resistance_d", CommonServantSkills::executeMagicResistanceD, "typemoonworld_core");
      registry.register("magic_resistance_a", CommonServantSkills::executeMagicResistanceA, "typemoonworld_core");
      registry.register("magic_resistance_b", CommonServantSkills::executeMagicResistanceB, "typemoonworld_core");
      registry.register("magic_resistance_c", CommonServantSkills::executeMagicResistanceC, "typemoonworld_core");
      registry.register("riding_b", CommonServantSkills::executeRidingB, "typemoonworld_core");
      registry.register("artoria_instinct_a", CommonServantSkills::executeArtoriaInstinctA, "typemoonworld_core");
      registry.register("mana_burst_a", CommonServantSkills::executeManaBurstA, "typemoonworld_core");
      registry.register("charisma_b", CommonServantSkills::executeCharismaB, "typemoonworld_core");
      registry.register("independent_action_a", CommonServantSkills::executeIndependentActionA, "typemoonworld_core");
      registry.register("independent_action_b", CommonServantSkills::executeIndependentActionB, "typemoonworld_core");
      registry.register("riding_a_plus", CommonServantSkills::executeRidingAPlus, "typemoonworld_core");
      registry.register("independent_action_c", CommonServantSkills::executeIndependentActionC, "typemoonworld_core");
      registry.register("divinity_e_minus", CommonServantSkills::executeDivinityEMinus, "typemoonworld_core");
      registry.register("battle_continuation_a", CommonServantSkills::executeBattleContinuationA, "typemoonworld_core");
      registry.register("emiya_style_battle_continuation", CommonServantSkills::executeEmiyaStyleBattleContinuation, "typemoonworld_core");
      registry.register("true_mind_eye_b", CommonServantSkills::executeTrueMindEyeB, "typemoonworld_core");
      registry.register("projection_magic_c", CommonServantSkills::executeProjectionMagicC, "typemoonworld_core");
      registry.register("clairvoyance_c", CommonServantSkills::executeClairvoyanceC, "typemoonworld_core");
      registry.register("clairvoyance_ex", CommonServantSkills::executeClairvoyanceEx, "typemoonworld_core");
      registry.register("stout_ex_arash", CommonServantSkills::executeStoutExArash, "typemoonworld_core");
      registry.register("clairvoyance_a_arash", CommonServantSkills::executeClairvoyanceAArash, "typemoonworld_core");
      registry.register("arrow_construction_a", CommonServantSkills::executeArrowConstructionA, "typemoonworld_core");
      registry.register("magic_resistance_c_arash", CommonServantSkills::executeMagicResistanceCArash, "typemoonworld_core");
      registry.register("strategy_b", CommonServantSkills::executeStrategyB, "typemoonworld_core");
      registry.register("tenka_fubu_a", CommonServantSkills::executeTenkaFubuA, "typemoonworld_core");
      registry.register("maou_a", CommonServantSkills::executeMaouA, "typemoonworld_core");
      registry.register("floating_matchlock", CommonServantSkills::executeOdaFloatingMatchlock, "typemoonworld_core");
      registry.register("matchlock_volley", CommonServantSkills::executeOdaMatchlockVolley, "typemoonworld_core");
      registry.register("fire_barrage", CommonServantSkills::executeOdaFireBarrage, "typemoonworld_core");
      registry.register("atsumori_step", CommonServantSkills::executeOdaAtsumoriStep, "typemoonworld_core");
      registry.register("anti_mystery_spark", CommonServantSkills::executeOdaAntiMysterySpark, "typemoonworld_core");
      registry.register("demon_king_pressure", CommonServantSkills::executeOdaDemonKingPressure, "typemoonworld_core");
      registry.register("hasebe_repel", CommonServantSkills::executeOdaHasebeRepel, "typemoonworld_core");
      registry.register("ash_field", CommonServantSkills::executeOdaAshField, "typemoonworld_core");
      registry.register("scorched_banner", CommonServantSkills::executeOdaScorchedBanner, "typemoonworld_core");
      registry.register("three_line_rotation", CommonServantSkills::executeOdaThreeLineRotation, "typemoonworld_core");
      registry.register("sunlit_pursuit", CommonServantSkills::executeGawainSunlitPursuit, "typemoonworld_core");
      registry.register("noon_guard", CommonServantSkills::executeGawainNoonGuard, "typemoonworld_core");
      registry.register("gallatin_spark", CommonServantSkills::executeGawainGallatinSpark, "typemoonworld_core");
      registry.register("solar_rebuke", CommonServantSkills::executeGawainSolarRebuke, "typemoonworld_core");
      registry.register("radiant_field", CommonServantSkills::executeGawainRadiantField, "typemoonworld_core");
      registry.register("flame_tornado", CommonServantSkills::executeGawainFlameTornado, "typemoonworld_core");
   }

   private static ServantExecutionResult executeMagicResistanceD(ServantExecutionContext context) {
      return applyMagicResistance(context.caster(), MagicResistanceRank.D, 0.10F, 0.0F);
   }

   private static ServantExecutionResult executeMagicResistanceA(ServantExecutionContext context) {
      return applyMagicResistance(context.caster(), MagicResistanceRank.A, 0.35F, 0.25F);
   }

   private static ServantExecutionResult executeMagicResistanceB(ServantExecutionContext context) {
      return applyMagicResistance(context.caster(), MagicResistanceRank.B, 0.25F, 0.175F);
   }

   private static ServantExecutionResult executeMagicResistanceC(ServantExecutionContext context) {
      return applyMagicResistance(context.caster(), MagicResistanceRank.C, 0.15F, 0.10F);
   }

   private static ServantExecutionResult executeMagicResistanceCArash(ServantExecutionContext context) {
      return applyMagicResistance(context.caster(), MagicResistanceRank.C, 0.20F, 0.10F);
   }

   private static ServantExecutionResult executeStoutExArash(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean("StoutExArashActive", true);
      entity.getPersistentData().putFloat("StoutExDamageMultiplier", 0.70F);
      entity.getPersistentData().putDouble("StoutDefenseRecoveryMultiplier", 2.0);
      entity.getPersistentData().putDouble("StoutPoiseRecoveryMultiplier", 2.0);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeClairvoyanceAArash(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean("ClairvoyanceAArashActive", true);
      entity.getPersistentData().putFloat("ClairvoyanceAccuracyBonus", 0.25F);
      entity.getPersistentData().putFloat("ClairvoyanceFutureSightDodge", 0.10F);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeArrowConstructionA(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean("ArrowConstructionAActive", true);
      entity.getPersistentData().putFloat("BowDamageMultiplier", 1.10F);
      entity.getPersistentData().putFloat("BowAttackSpeedMultiplier", 1.20F);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeRidingAPlus(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }

      entity.getPersistentData().putBoolean("RidingAPlusActive", true);
      entity.getPersistentData().putFloat("RidingAPlusSpeedBonus", 0.5F);
      entity.getPersistentData().putFloat("RidingAPlusArmorBonus", 0.2F);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeRidingB(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }

      entity.getPersistentData().putBoolean("RidingBActive", true);
      entity.getPersistentData().putFloat("RidingBSpeedBonus", 0.2F);
      entity.getPersistentData().putFloat("RidingBArmorBonus", 0.1F);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeGawainSunlitPursuit(ServantExecutionContext context) {
      return markGawainSkill(context, "GawainSunlitPursuitAvailable");
   }

   private static ServantExecutionResult executeGawainNoonGuard(ServantExecutionContext context) {
      return markGawainSkill(context, "GawainNoonGuardAvailable");
   }

   private static ServantExecutionResult executeGawainGallatinSpark(ServantExecutionContext context) {
      return markGawainSkill(context, "GawainGallatinSparkAvailable");
   }

   private static ServantExecutionResult executeGawainSolarRebuke(ServantExecutionContext context) {
      return markGawainSkill(context, "GawainSolarRebukeAvailable");
   }

   private static ServantExecutionResult executeGawainRadiantField(ServantExecutionContext context) {
      return markGawainSkill(context, "GawainRadiantFieldAvailable");
   }

   private static ServantExecutionResult executeGawainFlameTornado(ServantExecutionContext context) {
      return markGawainSkill(context, "GawainFlameTornadoAvailable");
   }

   private static ServantExecutionResult markGawainSkill(ServantExecutionContext context, String tag) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }
      entity.getPersistentData().putBoolean(tag, true);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeArtoriaInstinctA(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }

      entity.getPersistentData().putBoolean("ArtoriaInstinctAActive", true);
      entity.getPersistentData().putFloat("ArtoriaInstinctCertainHitNegationChance", 0.95F);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeManaBurstA(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }

      entity.getPersistentData().putBoolean("ArtoriaManaBurstAAvailable", true);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeCharismaB(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }

      entity.getPersistentData().putBoolean("ArtoriaCharismaBAvailable", true);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeIndependentActionB(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }

      entity.getPersistentData().putBoolean("IndependentActionActive", true);
      entity.getPersistentData().putFloat("IndependentActionCritDamageBonus", 0.08F);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeIndependentActionA(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean("IndependentActionActive", true);
      entity.getPersistentData().putFloat("IndependentActionCritDamageBonus", 0.12F);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeIndependentActionC(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }

      entity.getPersistentData().putBoolean("IndependentActionActive", true);
      entity.getPersistentData().putFloat("IndependentActionCritDamageBonus", 0.05F);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeDivinityEMinus(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }

      entity.getPersistentData().putBoolean("DivinityActive", true);
      entity.getPersistentData().putFloat("DivinityFlatDamage", 1.0F);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeBattleContinuationA(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }

      var data = entity.getPersistentData();
      data.putBoolean("BattleContinuationActive", true);
      if (!data.contains("BattleContinuationCooldown")) {
         data.putInt("BattleContinuationCooldown", 0);
      }
      data.putInt("BattleContinuationMaxCooldown", 6000);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeEmiyaStyleBattleContinuation(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (!(entity instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity)) {
         return ServantExecutionResult.FAILED;
      }

      entity.getPersistentData().putBoolean(
         net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity.EMIYA_CONTINUATION_ACTIVE,
         true
      );
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeTrueMindEyeB(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }

      entity.getPersistentData().putBoolean(net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroCombatHelper.MINDSEYE_ACTIVE_TAG, true);
      entity.getPersistentData().putFloat(net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroCombatHelper.MINDSEYE_DODGE_CHANCE_TAG, 0.55F);
      entity.getPersistentData().putFloat(net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroCombatHelper.MINDSEYE_BLOCK_CHANCE_TAG, 0.35F);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeProjectionMagicC(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }

      entity.getPersistentData().putBoolean("ProjectionMagicActive", true);
      entity.getPersistentData().putFloat("ProjectionMagicSwordDiscount", 0.5F);
      entity.getPersistentData().putInt("ProjectionMagicLifetimeTicks", 100);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeClairvoyanceC(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }

      entity.getPersistentData().putBoolean("ClairvoyanceActive", true);
      entity.getPersistentData().putFloat("ClairvoyanceAccuracyBonus", 0.30F);
      entity.getPersistentData().putFloat("ClairvoyanceCritBonus", 0.10F);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeClairvoyanceEx(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }
      entity.getPersistentData().putBoolean("ClairvoyanceExActive", true);
      entity.getPersistentData().putFloat("ClairvoyanceAccuracyBonus", 0.50F);
      entity.getPersistentData().putFloat("ClairvoyanceCritBonus", 0.20F);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeStrategyB(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }
      entity.getPersistentData().putBoolean("OdaStrategyBAvailable", true);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeTenkaFubuA(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }
      entity.getPersistentData().putBoolean("OdaTenkaFubuActive", true);
      entity.getPersistentData().putFloat("OdaTenkaFubuDivineBaseMultiplier", 2.0F);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeMaouA(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }
      entity.getPersistentData().putBoolean("OdaMaouAAvailable", true);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeOdaFloatingMatchlock(ServantExecutionContext context) {
      return markOdaSkill(context, "OdaFloatingMatchlockAvailable");
   }

   private static ServantExecutionResult executeOdaMatchlockVolley(ServantExecutionContext context) {
      return markOdaSkill(context, "OdaMatchlockVolleyAvailable");
   }

   private static ServantExecutionResult executeOdaFireBarrage(ServantExecutionContext context) {
      return markOdaSkill(context, "OdaFireBarrageAvailable");
   }

   private static ServantExecutionResult executeOdaAtsumoriStep(ServantExecutionContext context) {
      return markOdaSkill(context, "OdaAtsumoriStepAvailable");
   }

   private static ServantExecutionResult executeOdaAntiMysterySpark(ServantExecutionContext context) {
      return markOdaSkill(context, "OdaAntiMysterySparkAvailable");
   }

   private static ServantExecutionResult executeOdaDemonKingPressure(ServantExecutionContext context) {
      return markOdaSkill(context, "OdaDemonKingPressureAvailable");
   }

   private static ServantExecutionResult executeOdaHasebeRepel(ServantExecutionContext context) {
      return markOdaSkill(context, "OdaHasebeRepelAvailable");
   }

   private static ServantExecutionResult executeOdaAshField(ServantExecutionContext context) {
      return markOdaSkill(context, "OdaAshFieldAvailable");
   }

   private static ServantExecutionResult executeOdaScorchedBanner(ServantExecutionContext context) {
      return markOdaSkill(context, "OdaScorchedBannerAvailable");
   }

   private static ServantExecutionResult executeOdaThreeLineRotation(ServantExecutionContext context) {
      return markOdaSkill(context, "OdaThreeLineRotationAvailable");
   }

   private static ServantExecutionResult markOdaSkill(ServantExecutionContext context, String key) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }
      entity.getPersistentData().putBoolean(key, true);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult applyMagicResistance(LivingEntity entity, MagicResistanceRank rank, float damageReduction, float debuffResistance) {
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }

      MagicResistanceHelper.setMagicResistance(entity, rank, damageReduction, debuffResistance);
      return ServantExecutionResult.SUCCESS;
   }
}
