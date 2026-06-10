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
      registry.register("magic_resistance_b", CommonServantSkills::executeMagicResistanceB, "typemoonworld_core");
      registry.register("magic_resistance_c", CommonServantSkills::executeMagicResistanceC, "typemoonworld_core");
      registry.register("independent_action_b", CommonServantSkills::executeIndependentActionB, "typemoonworld_core");
      registry.register("riding_a_plus", CommonServantSkills::executeRidingAPlus, "typemoonworld_core");
      registry.register("independent_action_c", CommonServantSkills::executeIndependentActionC, "typemoonworld_core");
      registry.register("divinity_e_minus", CommonServantSkills::executeDivinityEMinus, "typemoonworld_core");
      registry.register("battle_continuation_a", CommonServantSkills::executeBattleContinuationA, "typemoonworld_core");
      registry.register("emiya_style_battle_continuation", CommonServantSkills::executeEmiyaStyleBattleContinuation, "typemoonworld_core");
      registry.register("true_mind_eye_b", CommonServantSkills::executeTrueMindEyeB, "typemoonworld_core");
      registry.register("projection_magic_c", CommonServantSkills::executeProjectionMagicC, "typemoonworld_core");
      registry.register("clairvoyance_c", CommonServantSkills::executeClairvoyanceC, "typemoonworld_core");
   }

   private static ServantExecutionResult executeMagicResistanceD(ServantExecutionContext context) {
      return applyMagicResistance(context.caster(), MagicResistanceRank.C, 0.15F, 0.0F);
   }

   private static ServantExecutionResult executeMagicResistanceB(ServantExecutionContext context) {
      return applyMagicResistance(context.caster(), MagicResistanceRank.B, 0.25F, 0.175F);
   }

   private static ServantExecutionResult executeMagicResistanceC(ServantExecutionContext context) {
      return applyMagicResistance(context.caster(), MagicResistanceRank.C, 0.15F, 0.10F);
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

   private static ServantExecutionResult executeIndependentActionB(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }

      entity.getPersistentData().putBoolean("IndependentActionActive", true);
      entity.getPersistentData().putFloat("IndependentActionCritDamageBonus", 0.08F);
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

   private static ServantExecutionResult applyMagicResistance(LivingEntity entity, MagicResistanceRank rank, float damageReduction, float debuffResistance) {
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }

      MagicResistanceHelper.setMagicResistance(entity, rank, damageReduction, debuffResistance);
      return ServantExecutionResult.SUCCESS;
   }
}
