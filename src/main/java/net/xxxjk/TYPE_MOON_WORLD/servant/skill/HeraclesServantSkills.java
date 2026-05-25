package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;

public final class HeraclesServantSkills {
   private HeraclesServantSkills() {
   }

   public static void registerBuiltin(ServantSkillRegistry registry) {
      registry.register("god_hand_passive", HeraclesServantSkills::executeGodHandPassive, "typemoonworld_core");
      registry.register("mad_enhancement_b", HeraclesServantSkills::executeMadEnhancement, "typemoonworld_core");
      registry.register("battle_continuation_a", HeraclesServantSkills::executeBattleContinuation, "typemoonworld_core");
      registry.register("valor_a_plus", HeraclesServantSkills::executeValor, "typemoonworld_core");
      registry.register("false_mind_eye_b", HeraclesServantSkills::executeFalseMindEye, "typemoonworld_core");
      registry.register("divinity_a", HeraclesServantSkills::executeDivinity, "typemoonworld_core");
   }

   private static ServantExecutionResult executeGodHandPassive(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean("GodHandActive", true);
      entity.getPersistentData().putFloat("GodHandThreshold", 3.0F);
      entity.getPersistentData().putInt("GodHandLives", 11);
      entity.getPersistentData().putFloat("GodHandAdaptiveReduction", 0.25F);
      entity.getPersistentData().putFloat("GodHandAdaptiveMax", 0.75F);
      entity.getPersistentData().putInt("GodHandStrongCost", 2);
      entity.getPersistentData().putInt("GodHandExtraStrongCost", 3);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeMadEnhancement(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean("MadEnhancementActive", true);
      entity.getPersistentData().putFloat("MadEnhancementPenalty", 0.3F);
      entity.getPersistentData().putFloat("MadEnhancementDamageBonus", 0.15F);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeBattleContinuation(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean("BattleContinuationActive", true);
      entity.getPersistentData().putInt("BattleContinuationCooldown", 0);
      entity.getPersistentData().putInt("BattleContinuationMaxCooldown", 6000);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeValor(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1, false, false, true));
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeFalseMindEye(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1, false, false, true));
      entity.getPersistentData().putBoolean("CritDamageBoostActive", true);
      entity.getPersistentData().putFloat("CritDamageMultiplier", 2.0F);
      entity.getPersistentData().putInt("CritDamageTicksRemaining", 200);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeDivinity(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean("DivinityActive", true);
      entity.getPersistentData().putFloat("DivinityFlatDamage", 25.0F);
      return ServantExecutionResult.SUCCESS;
   }
}
