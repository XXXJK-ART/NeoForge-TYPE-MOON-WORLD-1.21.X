package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SenkoMuramasaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MuramasaCombatHelper;
import net.minecraft.server.level.ServerLevel;

public final class MuramasaServantSkills {
   private MuramasaServantSkills() {
   }

   public static void registerBuiltin(ServantSkillRegistry registry) {
      registry.register("trial_slash_b_plus", MuramasaServantSkills::trialSlash, "typemoonworld_core");
      registry.register("karma_eye_a", MuramasaServantSkills::karmaEye, "typemoonworld_core");
      registry.register("flame_ex", MuramasaServantSkills::flame, "typemoonworld_core");
      registry.register("tsumukari_muramasa_ex", MuramasaServantSkills::tsumukari, "typemoonworld_core");
      registry.register("projection_volley_b", MuramasaServantSkills::projectionVolley, "typemoonworld_core");
      registry.register("forge_temper_b", MuramasaServantSkills::forgeTemper, "typemoonworld_core");
      registry.register("karma_sever_b", MuramasaServantSkills::karmaSever, "typemoonworld_core");
      registry.register("sword_field_prototype", MuramasaServantSkills::swordField, "typemoonworld_core");
      registry.register("territory_creation_a", MuramasaServantSkills::territory, "typemoonworld_core");
      registry.register("sword_aesthetics_a", MuramasaServantSkills::aesthetics, "typemoonworld_core");
      registry.register("contemporary_unluck_b", MuramasaServantSkills::unluck, "typemoonworld_core");
   }

   private static ServantExecutionResult trialSlash(ServantExecutionContext context) {
      if (!(context.caster() instanceof SenkoMuramasaEntity entity)) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean("MuramasaTrialAvailable", true);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult karmaEye(ServantExecutionContext context) {
      if (!(context.caster() instanceof SenkoMuramasaEntity entity)) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean("MuramasaKarmaEyeAvailable", true);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult flame(ServantExecutionContext context) {
      if (!(context.caster() instanceof SenkoMuramasaEntity entity)) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean("MuramasaFlameAvailable", true);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult tsumukari(ServantExecutionContext context) {
      if (!(context.caster() instanceof SenkoMuramasaEntity entity)) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean("MuramasaTsumukariAvailable", true);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult projectionVolley(ServantExecutionContext context) {
      if (!(context.caster() instanceof SenkoMuramasaEntity entity)
         || !(entity.level() instanceof ServerLevel level)
         || !(context.target() instanceof net.minecraft.world.entity.LivingEntity target)) {
         return ServantExecutionResult.FAILED;
      }
      return MuramasaCombatHelper.performProjectionVolley(entity, target, level)
         ? ServantExecutionResult.SUCCESS : ServantExecutionResult.FAILED;
   }

   private static ServantExecutionResult forgeTemper(ServantExecutionContext context) {
      return context.caster() instanceof SenkoMuramasaEntity entity
         && MuramasaCombatHelper.performTemper(entity)
         ? ServantExecutionResult.SUCCESS : ServantExecutionResult.FAILED;
   }

   private static ServantExecutionResult karmaSever(ServantExecutionContext context) {
      if (!(context.caster() instanceof SenkoMuramasaEntity entity)
         || !(entity.level() instanceof ServerLevel level)
         || !(context.target() instanceof net.minecraft.world.entity.LivingEntity target)) {
         return ServantExecutionResult.FAILED;
      }
      return MuramasaCombatHelper.performKarmaSlash(entity, target, level)
         ? ServantExecutionResult.SUCCESS : ServantExecutionResult.FAILED;
   }

   private static ServantExecutionResult swordField(ServantExecutionContext context) {
      return context.caster() instanceof SenkoMuramasaEntity entity
         && entity.level() instanceof ServerLevel level
         && MuramasaCombatHelper.performSwordField(entity, level)
         ? ServantExecutionResult.SUCCESS : ServantExecutionResult.FAILED;
   }

   private static ServantExecutionResult territory(ServantExecutionContext context) {
      return context.caster() == null ? ServantExecutionResult.FAILED : ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult aesthetics(ServantExecutionContext context) {
      return context.caster() == null ? ServantExecutionResult.FAILED : ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult unluck(ServantExecutionContext context) {
      return context.caster() == null ? ServantExecutionResult.FAILED : ServantExecutionResult.SUCCESS;
   }
}
