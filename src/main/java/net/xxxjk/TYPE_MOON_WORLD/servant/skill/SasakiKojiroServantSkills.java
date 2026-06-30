package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroCombatHelper;

public final class SasakiKojiroServantSkills {
   private SasakiKojiroServantSkills() {
   }

   public static void registerBuiltin(ServantSkillRegistry registry) {
      registry.register("stealth_d", SasakiKojiroServantSkills::executeStealth, "typemoonworld_core");
      registry.register("spiritualization_b_plus", SasakiKojiroServantSkills::executeSpiritualization, "typemoonworld_core");
      registry.register("souwa_expertise_b", SasakiKojiroServantSkills::executeSouwaExpertise, "typemoonworld_core");
      registry.register("mindseye_a", SasakiKojiroServantSkills::executeMindseye, "typemoonworld_core");
   }

   private static ServantExecutionResult executeStealth(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean("StealthPassiveActive", true);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeSpiritualization(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      java.util.Iterator<MobEffectInstance> it = entity.getActiveEffects().iterator();
      while (it.hasNext()) {
         MobEffectInstance eff = it.next();
         if (eff.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            entity.removeEffect(eff.getEffect());
         }
      }
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3, false, false, true));
      entity.getPersistentData().putBoolean("SpiritualizationActive", true);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeSouwaExpertise(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean("SouwaExpertiseActive", true);
      return ServantExecutionResult.SUCCESS;
   }

   private static ServantExecutionResult executeMindseye(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;
      entity.getPersistentData().putBoolean(SasakiKojiroCombatHelper.MINDSEYE_ACTIVE_TAG, true);
      entity.getPersistentData().putFloat(SasakiKojiroCombatHelper.MINDSEYE_DODGE_CHANCE_TAG, 0.8F);
      entity.getPersistentData().putFloat(SasakiKojiroCombatHelper.MINDSEYE_BLOCK_CHANCE_TAG, 0.4F);
      return ServantExecutionResult.SUCCESS;
   }
}
