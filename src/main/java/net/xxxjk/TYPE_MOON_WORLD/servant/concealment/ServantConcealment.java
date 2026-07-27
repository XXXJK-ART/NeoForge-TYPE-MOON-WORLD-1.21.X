package net.xxxjk.TYPE_MOON_WORLD.servant.concealment;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.martial.BajiquanCombatService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ShadowHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanRules;

/** Shared server-side authority for servant-grade complete concealment. */
public final class ServantConcealment {
   private static final String TAG_MANAGED_SILENCE = "TypeMoonConcealmentManagedSilence";
   private static final String TAG_PREVIOUS_SILENCE = "TypeMoonConcealmentPreviousSilence";

   private ServantConcealment() {
   }

   public static boolean isFullyConcealed(LivingEntity entity) {
      if (entity instanceof ShadowHassanEntity hassan) return hassan.isPresenceConcealed();
      if (entity instanceof ServantEntity) return entity.isInvisible();
      if (!(entity instanceof Player player)) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (BajiquanCombatService.isCircleRealmActive(player)) return true;
      if (!vars.servant_card_transformed) return false;
      if ("shadow_hassan".equals(vars.servant_card_id)) {
         MobEffectInstance invisibility = player.getEffect(MobEffects.INVISIBILITY);
         return invisibility != null && invisibility.getAmplifier() >= 1;
      }
      return player.isInvisible();
   }

   public static void tick(LivingEntity entity) {
      boolean concealed = isFullyConcealed(entity);
      manageSilence(entity, concealed);
      if (!concealed) return;

      if (entity.tickCount % 5 == 0 && entity.level() instanceof ServerLevel level) {
         for (Mob mob : level.getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(48.0),
            mob -> mob != entity && mob.getTarget() == entity)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
         }
      }
      if (entity.tickCount % 20 == 0) suppressRevealingEffects(entity);
   }

   public static void suppressRevealingEffects(LivingEntity entity) {
      entity.setGlowingTag(false);
      if (entity.level().isClientSide()) return;
      for (MobEffectInstance effect : List.copyOf(entity.getActiveEffects())) {
         if (effect.getEffect().is(ShadowHassanRules.REVEALING_EFFECTS)) entity.removeEffect(effect.getEffect());
      }
   }

   private static void manageSilence(LivingEntity entity, boolean concealed) {
      if (entity instanceof ShadowHassanEntity) return;
      var data = entity.getPersistentData();
      if (concealed) {
         if (!data.getBoolean(TAG_MANAGED_SILENCE)) {
            data.putBoolean(TAG_MANAGED_SILENCE, true);
            data.putBoolean(TAG_PREVIOUS_SILENCE, entity.isSilent());
         }
         entity.setSilent(true);
      } else if (data.getBoolean(TAG_MANAGED_SILENCE)) {
         entity.setSilent(data.getBoolean(TAG_PREVIOUS_SILENCE));
         data.remove(TAG_MANAGED_SILENCE);
         data.remove(TAG_PREVIOUS_SILENCE);
      }
   }
}
