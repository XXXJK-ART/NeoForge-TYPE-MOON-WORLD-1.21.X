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
      registry.register("magic_resistance_b", CommonServantSkills::executeMagicResistanceB, "typemoonworld_core");
      registry.register("magic_resistance_c", CommonServantSkills::executeMagicResistanceC, "typemoonworld_core");
      registry.register("riding_a_plus", CommonServantSkills::executeRidingAPlus, "typemoonworld_core");
      registry.register("independent_action_c", CommonServantSkills::executeIndependentActionC, "typemoonworld_core");
      registry.register("divinity_e_minus", CommonServantSkills::executeDivinityEMinus, "typemoonworld_core");
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

   private static ServantExecutionResult applyMagicResistance(LivingEntity entity, MagicResistanceRank rank, float damageReduction, float debuffResistance) {
      if (entity == null) {
         return ServantExecutionResult.FAILED;
      }

      MagicResistanceHelper.setMagicResistance(entity, rank, damageReduction, debuffResistance);
      return ServantExecutionResult.SUCCESS;
   }
}
