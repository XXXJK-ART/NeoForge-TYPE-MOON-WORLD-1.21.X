package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.concealment.ServantConcealment;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardShadowHassanSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ShadowHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanPursuitData;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class ShadowHassanEvents {
   private ShadowHassanEvents() {
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onTargetChange(LivingChangeTargetEvent event) {
      if (event.getNewAboutToBeSetTarget() instanceof LivingEntity target && ServantConcealment.isFullyConcealed(target)) {
         event.setNewAboutToBeSetTarget(null);
      }
   }

   @SubscribeEvent
   public static void hideInvisibilityParticles(EffectParticleModificationEvent event) {
      if (ServantConcealment.isFullyConcealed(event.getEntity())) event.setVisible(false);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void rejectRevealingEffects(MobEffectEvent.Applicable event) {
      if (ServantConcealment.isFullyConcealed(event.getEntity())
         && event.getEffectInstance().getEffect().is(net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanRules.REVEALING_EFFECTS)) {
         event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onLivingDeath(LivingDeathEvent event) {
      if (event.isCanceled() || event.getEntity().level().getServer() == null) return;
      if (event.getEntity() instanceof ServerPlayer master) {
         LivingEntity killer = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
         ServantCardShadowHassanSkills.onPlayerDeath(master, killer);
         ShadowHassanPursuitData.get(master.getServer()).onMasterDeath(master, killer);
      }
      ShadowHassanPursuitData.get(event.getEntity().level().getServer())
         .removePursuitsForTarget(event.getEntity().level().getServer(), event.getEntity().getUUID());
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent.Post event) {
      ShadowHassanPursuitData.get(event.getServer()).tick(event.getServer());
   }
}
