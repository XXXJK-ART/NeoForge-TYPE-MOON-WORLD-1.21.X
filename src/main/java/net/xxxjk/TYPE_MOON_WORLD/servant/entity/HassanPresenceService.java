package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.concealment.ServantConcealment;

/** Toggle state; rank, leakage, silence and visibility use the shared concealment controller. */
public final class HassanPresenceService {
   private static final String ENABLED = "CursedArmPresenceEnabled";
   private HassanPresenceService() { }

   public static boolean isHassan(LivingEntity entity) {
      var definition = ServantIdentityHelper.definitionOf(entity);
      return entity instanceof CursedArmHassanEntity || definition != null && "cursed_arm_hassan".equals(definition.id());
   }

   public static boolean enabled(LivingEntity entity) { return entity.getPersistentData().getBoolean(ENABLED); }

   public static boolean set(LivingEntity entity, boolean enabled) {
      if (entity.level().isClientSide() || !isHassan(entity)) return false;
      if (enabled && !ServantConcealment.canActivatePresenceConcealment(entity)) return false;
      entity.getPersistentData().putBoolean(ENABLED, enabled);
      if (enabled) tick(entity);
      else {
         entity.removeEffect(MobEffects.INVISIBILITY);
         entity.setInvisible(false);
      }
      return true;
   }

   public static void tick(LivingEntity entity) {
      if (!enabled(entity)) return;
      entity.getPersistentData().remove("StealthPassiveActive");
      var effect = entity.getEffect(MobEffects.INVISIBILITY);
      if (effect == null || effect.getDuration() < 20)
         entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
   }
}
