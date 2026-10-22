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
      for(String rank:java.util.List.of("E","D","C","B","A")) {
         registry.register("recast_stance_"+rank.toLowerCase(java.util.Locale.ROOT),context->{
            if(context.caster()==null)return ServantExecutionResult.FAILED;
            if(net.xxxjk.TYPE_MOON_WORLD.servant.cu.CuChulainnAbilityService.isOwner(context.caster()) && rank.equals("C"))
               return net.xxxjk.TYPE_MOON_WORLD.servant.cu.CuChulainnAbilityService.start(context.caster(),5)?ServantExecutionResult.SUCCESS:ServantExecutionResult.FAILED;
            net.xxxjk.TYPE_MOON_WORLD.servant.combat.DisengageService.apply(context.caster(),rank);
            return ServantExecutionResult.SUCCESS;
         },"typemoonworld_core");
      }
      registry.register("four_branches_a",context->context.caster()==null?ServantExecutionResult.FAILED:ServantExecutionResult.SUCCESS,"typemoonworld_core");
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
      var data = entity.getPersistentData();
      data.putBoolean(CuChulainnCombatHelper.RECAST_STANCE_READY_TAG, true);
      if (!data.contains(CuChulainnCombatHelper.RECAST_STANCE_USED_TAG)) {
         data.putBoolean(CuChulainnCombatHelper.RECAST_STANCE_USED_TAG, false);
      }
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeDivinity(ServantExecutionContext context) {
      if (context.caster() == null) return ServantExecutionResult.FAILED;
      net.xxxjk.TYPE_MOON_WORLD.servant.combat.CommonSkillService.reconcile(context.caster());
      return ServantExecutionResult.SUCCESS;
   }
}
