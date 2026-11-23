package net.xxxjk.TYPE_MOON_WORLD.servant.perception;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

@EventBusSubscriber(modid = "typemoonworld")
public final class ServantPassiveService {
   private static final String NIGHT_VISION_TAG = "TypeMoonServantNightVision";
   private static final String LAST_DURATION_TAG = "TypeMoonServantNightVisionDuration";
   private static final int NIGHT_VISION_DURATION_TICKS = 1200;
   private static final int NIGHT_VISION_REFRESH_THRESHOLD_TICKS = 200;
   private ServantPassiveService() { }

   @SubscribeEvent public static void tick(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()) return;
      boolean servant = entity instanceof ServantEntity || entity instanceof ServerPlayer player
         && player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_transformed;
      var data = entity.getPersistentData();
      if (servant) {
         MobEffectInstance current = entity.getEffect(MobEffects.NIGHT_VISION);
         if (!data.getBoolean(NIGHT_VISION_TAG)) {
            if (current != null) return;
            data.putBoolean(NIGHT_VISION_TAG, true);
            entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION_TICKS, 0, true, false, false));
            data.putInt(LAST_DURATION_TAG, NIGHT_VISION_DURATION_TICKS);
            return;
         }
         int last = data.getInt(LAST_DURATION_TAG);
         if (current != null && (current.getAmplifier() != 0 || last > 0 && current.getDuration() > last + 1)) {
            data.remove(NIGHT_VISION_TAG);
            data.remove(LAST_DURATION_TAG);
            return;
         }
         if (current == null || current.getDuration() <= NIGHT_VISION_REFRESH_THRESHOLD_TICKS) {
            entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION_TICKS, 0, true, false, false));
            current = entity.getEffect(MobEffects.NIGHT_VISION);
         }
         if (current != null) data.putInt(LAST_DURATION_TAG, current.getDuration());
      } else if (data.getBoolean(NIGHT_VISION_TAG)) {
         data.remove(NIGHT_VISION_TAG);
         MobEffectInstance current = entity.getEffect(MobEffects.NIGHT_VISION);
         if (current != null && current.getDuration() <= NIGHT_VISION_DURATION_TICKS && current.getAmplifier() == 0) entity.removeEffect(MobEffects.NIGHT_VISION);
         data.remove(LAST_DURATION_TAG);
      }
   }

   @SubscribeEvent public static void death(LivingDeathEvent event) {
      LivingEntity entity = event.getEntity();
      if (entity.getPersistentData().getBoolean(NIGHT_VISION_TAG)) {
         entity.getPersistentData().remove(NIGHT_VISION_TAG);
         MobEffectInstance current = entity.getEffect(MobEffects.NIGHT_VISION);
         if (current != null && current.getDuration() <= NIGHT_VISION_DURATION_TICKS && current.getAmplifier() == 0) entity.removeEffect(MobEffects.NIGHT_VISION);
         entity.getPersistentData().remove(LAST_DURATION_TAG);
      }
   }

   @SubscribeEvent public static void respawn(PlayerRespawnEvent event) {
      var data = event.getEntity().getPersistentData();
      if (data.getBoolean(NIGHT_VISION_TAG)) event.getEntity().removeEffect(MobEffects.NIGHT_VISION);
      data.remove(NIGHT_VISION_TAG);
      data.remove(LAST_DURATION_TAG);
   }
}
