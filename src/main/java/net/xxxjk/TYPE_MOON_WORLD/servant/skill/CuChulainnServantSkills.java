package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper;

public final class CuChulainnServantSkills {
   private CuChulainnServantSkills() {
   }

   public static void registerBuiltin(ServantSkillRegistry registry) {
      registry.register("protection_from_arrows_b", CuChulainnServantSkills::executeProtectionFromArrows, "typemoonworld_core");
      registry.register("recast_stance_c", CuChulainnServantSkills::executeRecastStance, "typemoonworld_core");
      registry.register("divinity_b", CuChulainnServantSkills::executeDivinity, "typemoonworld_core");
   }

   private static ServantExecutionResult executeProtectionFromArrows(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean(CuChulainnCombatHelper.PROTECTION_FROM_ARROWS_TAG, true);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeRecastStance(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean(CuChulainnCombatHelper.RECAST_STANCE_READY_TAG, true);
      entity.getPersistentData().putBoolean(CuChulainnCombatHelper.RECAST_STANCE_USED_TAG, false);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeDivinity(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean("DivinityActive", true);
      entity.getPersistentData().putFloat("DivinityFlatDamage", 3.0F);
      return ServantExecutionResult.SUCCESS;
   }
}
