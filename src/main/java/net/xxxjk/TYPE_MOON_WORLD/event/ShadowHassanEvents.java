package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
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
      if (event.getNewAboutToBeSetTarget() instanceof LivingEntity target
         && ServantConcealment.suppressesAggroFrom(target, event.getEntity())) {
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
         if (ServantCardShadowHassanSkills.isPossessing(master)) {
            ServantCardShadowHassanSkills.forceEndPossession(master, "player_death", false);
         }
         LivingEntity killer = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
         if (killer == null && event.getSource() instanceof net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanDamageSource hidden
            && master.level() instanceof net.minecraft.server.level.ServerLevel level) {
            killer = hidden.resolveAttacker(level);
         }
         ServantCardShadowHassanSkills.onPlayerDeath(master, killer);
         ShadowHassanPursuitData.get(master.getServer()).onMasterDeath(master, killer);
      }
      if (event.getEntity().level().getServer() != null) {
         for (ServerPlayer player : event.getEntity().level().getServer().getPlayerList().getPlayers()) {
            if (ServantCardShadowHassanSkills.isPossessingHost(player, event.getEntity().getUUID())) {
               ServantCardShadowHassanSkills.forceEndPossession(player, "host_death", true);
            }
         }
      }
      if (event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel level) {
         for (ShadowHassanEntity hassan : level.getEntitiesOfClass(ShadowHassanEntity.class,
            event.getEntity().getBoundingBox().inflate(18.0))) {
            hassan.releasePossessionForHost(event.getEntity().getUUID());
         }
      }
      ShadowHassanPursuitData.get(event.getEntity().level().getServer())
         .removePursuitsForTarget(event.getEntity().level().getServer(), event.getEntity().getUUID());
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent.Post event) {
      ShadowHassanPursuitData.get(event.getServer()).tick(event.getServer());
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void blockPossessedPlayerInput(AttackEntityEvent event) {
      if (event.getEntity() instanceof ServerPlayer player
         && ServantCardShadowHassanSkills.blocksPlayerInput(player)) event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void blockPossessedItemInteraction(PlayerInteractEvent.RightClickItem event) {
      if (event.getEntity() instanceof ServerPlayer player
         && ServantCardShadowHassanSkills.blocksPlayerInput(player)) event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void blockPossessedBlockInteraction(PlayerInteractEvent.RightClickBlock event) {
      if (event.getEntity() instanceof ServerPlayer player
         && ServantCardShadowHassanSkills.blocksPlayerInput(player)) event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void blockPossessedBlockMining(PlayerInteractEvent.LeftClickBlock event) {
      if (event.getEntity() instanceof ServerPlayer player
         && ServantCardShadowHassanSkills.blocksPlayerInput(player)) event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void blockPossessedEntityInteraction(PlayerInteractEvent.EntityInteract event) {
      if (event.getEntity() instanceof ServerPlayer player
         && ServantCardShadowHassanSkills.blocksPlayerInput(player)) event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void routePossessedDamage(LivingIncomingDamageEvent event) {
      if (!(event.getEntity() instanceof ServerPlayer player)
         || !ServantCardShadowHassanSkills.isPossessing(player) || event.getAmount() <= 0.0F) return;
      Entity source = event.getSource().getEntity();
      if (source instanceof LivingEntity host && host.getUUID().equals(
         player.getPersistentData().getUUID("ShadowHassanPossessionHost"))
         && net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanRules.isOrdinaryHostAttack(event.getSource(), host)) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void releasePossessionAfterDamage(LivingDamageEvent.Post event) {
      if (event.getNewDamage() > 0.0F && event.getEntity() instanceof ServerPlayer player) {
         ServantCardShadowHassanSkills.onDamageApplied(player);
      }
   }

   @SubscribeEvent
   public static void clearPossessionOnDimensionChange(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && ServantCardShadowHassanSkills.isPossessing(player)) {
         ServantCardShadowHassanSkills.forceEndPossession(player, "dimension_changed", true);
      }
   }

   @SubscribeEvent
   public static void clearPossessionOnLogout(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && ServantCardShadowHassanSkills.isPossessing(player)) {
         ServantCardShadowHassanSkills.forceEndPossession(player, "logout", false);
      }
   }
}
