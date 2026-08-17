package net.xxxjk.TYPE_MOON_WORLD.event;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.OwnedPaleRiderMob;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderCombatRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderCrowEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.MuramasaDamageTypes;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class PaleRiderEvents {
   private static final Map<DamageContainer, Boolean> REDIRECTED_CARD_DAMAGE = Collections.synchronizedMap(new WeakHashMap<>());

   private PaleRiderEvents() {
   }

   @SubscribeEvent
   public static void onEntityTick(EntityTickEvent.Post event) {
      if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide()
         && living.getPersistentData().contains(PaleRiderInfectionService.TAG_LEVEL)) {
         PaleRiderInfectionService.tick(living);
      }
      if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide()) {
         if (PaleRiderCombatHelper.hasExpiredPenaltyMarkers(living)) {
            PaleRiderCombatHelper.cleanupExpiredPenalties(living);
         }
         if (living instanceof OwnedPaleRiderMob || living instanceof PaleRiderCrowEntity
            || living.getPersistentData().getBoolean(PaleRiderInfectionService.TAG_CONTROLLED)) {
            net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderCorruptionService.tickFootsteps(living);
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onControlledFriendlyFire(LivingIncomingDamageEvent event) {
      if (event.getSource().is(MuramasaDamageTypes.TSUMUKARI_MURAMASA)) return;
      Entity attacker = event.getSource().getEntity();
      Entity direct = event.getSource().getDirectEntity();
      if (PaleRiderInfectionService.arePaleRiderAllies(event.getEntity(), attacker)
         || PaleRiderInfectionService.arePaleRiderAllies(event.getEntity(), direct)) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
   public static void onInfectionDamageStart(LivingIncomingDamageEvent event) {
      if (!net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes.isInfection(event.getSource())) return;
      if (PaleRiderInfectionService.isPaleRiderCardPlayer(event.getEntity())) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         return;
      }
      if (net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(event.getEntity())) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         return;
      }
      event.setCanceled(false);
      event.setAmount(event.getOriginalAmount());
      event.setInvulnerabilityTicks(0);
      for (DamageContainer.Reduction reduction : DamageContainer.Reduction.values()) {
         event.addReductionModifier(reduction, (container, amount) -> 0.0F);
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
   public static void onInfectionDamageEnd(LivingIncomingDamageEvent event) {
      if (!net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes.isInfection(event.getSource())) return;
      if (PaleRiderInfectionService.isPaleRiderCardPlayer(event.getEntity())
         || net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(event.getEntity())) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         return;
      }
      event.setCanceled(false);
      event.setAmount(event.getOriginalAmount());
      event.setInvulnerabilityTicks(0);
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onInfectionFinalDamage(LivingDamageEvent.Pre event) {
      if (net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes.isInfection(event.getSource())) {
         event.setNewDamage(event.getOriginalDamage());
      }
      if (PaleRiderDamageTypes.isPaleRiderDamage(event.getSource())) {
         ServantDefinition definition = ServantIdentityHelper.definitionOf(event.getEntity());
         String servantId = definition == null ? "" : definition.id();
         event.setNewDamage(event.getNewDamage() * PaleRiderCombatRules.plagueSpecialAttackMultiplier(servantId));
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onPossessedRiderDamage(LivingIncomingDamageEvent event) {
      if (event.getSource().is(MuramasaDamageTypes.TSUMUKARI_MURAMASA)) return;
      if (event.isCanceled() || !(event.getEntity() instanceof PaleRiderEntity rider) || !rider.hasPossessedHost()) return;
      rider.redirectPossessedDamage(event.getSource(), event.getAmount());
      event.setAmount(0.0F);
      event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onPossessedCardDamage(LivingIncomingDamageEvent event) {
      if (event.getSource().is(MuramasaDamageTypes.TSUMUKARI_MURAMASA)) return;
      if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) return;
      var vars = player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !"pale_rider".equals(vars.servant_card_id)) return;
      if (net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardPaleRiderSkills.redirectPossessedDamage(player, event.getSource(), event.getAmount())) {
         REDIRECTED_CARD_DAMAGE.put(event.getContainer(), Boolean.TRUE);
         event.setAmount(0.0F);
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
   public static void onPossessedCardDamageFinal(LivingIncomingDamageEvent event) {
      if (REDIRECTED_CARD_DAMAGE.remove(event.getContainer()) == null) return;
      event.setAmount(0.0F);
      event.setCanceled(true);
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onPaleRiderMeleeInfection(LivingIncomingDamageEvent event) {
      if (!event.isCanceled() && event.getSource().is(DamageTypes.MOB_ATTACK)
         && event.getSource().getEntity() instanceof PaleRiderEntity rider
         && !net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderCombatHelper.isMinorSkillDamage(rider)) {
         PaleRiderInfectionService.infect(event.getEntity(), rider, 2);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onControlledTargetChange(LivingChangeTargetEvent event) {
      if (event.getNewAboutToBeSetTarget() instanceof PaleRiderEntity rider && rider.getPossessedHost() != null) {
         event.setNewAboutToBeSetTarget(rider.getPossessedHost());
         return;
      }
      if (event.getEntity() instanceof Mob mob
         && PaleRiderInfectionService.isUncontrollableServantSoldier(mob)
         && PaleRiderInfectionService.isControlled(mob)) {
         PaleRiderInfectionService.releaseControl(mob);
         event.setNewAboutToBeSetTarget(null);
         return;
      }
      if (event.getEntity() instanceof Mob mob && PaleRiderInfectionService.isControlled(mob)) {
         event.setNewAboutToBeSetTarget(PaleRiderInfectionService.getCommandTarget(mob));
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onLivingDeath(LivingDeathEvent event) {
      if (event.isCanceled() || !(event.getEntity().level() instanceof ServerLevel level)) {
         return;
      }
      if (event.getEntity() instanceof ServerPlayer deadPlayer
         && PaleRiderInfectionService.isPaleRiderCardPlayer(deadPlayer)) {
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardPaleRiderSkills.returnAllLivingSouls(deadPlayer, level);
      }
      LivingEntity owner = PaleRiderInfectionService.isInfected(event.getEntity())
         ? validOwner(PaleRiderInfectionService.getOwner(level, event.getEntity())) : null;
      if (owner == null) {
         owner = resolveOwner(level, event.getSource().getEntity());
      }
      if (owner == null) {
         owner = resolveOwner(level, event.getSource().getDirectEntity());
      }
      if (owner instanceof PaleRiderEntity rider && rider.isAlive()) rider.captureSoul(event.getEntity());
      else if (owner instanceof ServerPlayer player && player.isAlive()) {
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardPaleRiderSkills.captureSoul(player, event.getEntity());
      }
   }

   @SubscribeEvent
   public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
      if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null
         || !PaleRiderInfectionService.isPaleRiderCardPlayer(player)) return;
      ServerLevel previousLevel = player.getServer().getLevel(event.getFrom());
      if (previousLevel != null) {
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardPaleRiderSkills.cleanupPreviousLevel(player, previousLevel);
      }
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level
         && PaleRiderInfectionService.isPaleRiderCardPlayer(player)) {
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardPaleRiderSkills.cleanupPreviousLevel(player, level);
      }
   }

   @SubscribeEvent
   public static void onLevelUnload(LevelEvent.Unload event) {
      if (event.getLevel() instanceof ServerLevel level) {
         net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderEntityIndex.clearLevel(level);
      }
   }

   private static LivingEntity resolveOwner(ServerLevel level, Entity source) {
      LivingEntity directOwner = source instanceof LivingEntity living ? validOwner(living) : null;
      if (directOwner != null) return directOwner;
      if (source instanceof OwnedPaleRiderMob owned) {
         return validOwner(owned.getPaleRiderLivingOwner());
      }
      if (source instanceof PaleRiderCrowEntity crow) {
         return validOwner(crow.getPaleRiderLivingOwner());
      }
      if (source instanceof Projectile projectile && projectile.getOwner() != source) {
         LivingEntity projectileOwner = resolveOwner(level, projectile.getOwner());
         if (projectileOwner != null) {
            return projectileOwner;
         }
      }
      if (source != null && source.getPersistentData().getBoolean(PaleRiderInfectionService.TAG_CONTROLLED)
         && source.getPersistentData().hasUUID(PaleRiderInfectionService.TAG_OWNER)
         && level.getEntity(source.getPersistentData().getUUID(PaleRiderInfectionService.TAG_OWNER)) instanceof LivingEntity owner) {
         return validOwner(owner);
      }
      return null;
   }

   private static LivingEntity validOwner(LivingEntity owner) {
      if (owner instanceof PaleRiderEntity) return owner;
      return owner instanceof ServerPlayer player && PaleRiderInfectionService.isPaleRiderCardPlayer(player) ? player : null;
   }
}
