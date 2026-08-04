package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmDamageClassifier;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.NightingaleEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardNightingaleSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.nightingale.NightingaleDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.nightingale.NightingaleHumanoidHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.nightingale.NightingaleRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.nightingale.NightingaleSupportService;
import net.xxxjk.TYPE_MOON_WORLD.magic.MuramasaDamageTypes;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class NightingaleEvents {
   private NightingaleEvents() {}

   @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
   public static void protectSafetyCircleStart(LivingIncomingDamageEvent event) {
      if (shouldCancelSafetyCircleDamage(event.getEntity(), event.getSource())) cancel(event);
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void applyCombatModifiers(LivingIncomingDamageEvent event) {
      if (event.isCanceled() || event.getSource().is(NightingaleDamageTypes.HEALING_REVERSAL)) return;
      LivingEntity target = event.getEntity();
      LivingEntity attacker = resolveAttacker(event.getSource());
      float amount = event.getAmount();
      if (attacker != null && NightingaleSupportService.isNightingaleSource(attacker)) {
         amount *= NightingaleRules.outgoingMultiplier(
            NightingaleHumanoidHelper.isHumanoid(target), NightingaleSupportService.hasAngelCry(attacker, target.level().getGameTime())
         );
      } else if (attacker != null && NightingaleSupportService.hasAngelCry(attacker, target.level().getGameTime())) {
         amount *= NightingaleRules.ANGEL_DAMAGE_MULTIPLIER;
      }
      if (NightingaleSupportService.isNightingaleSource(target)
         && attacker != null && NightingaleHumanoidHelper.isHumanoid(attacker)) {
         amount *= NightingaleRules.HUMANOID_DEFENSE_MULTIPLIER;
      }
      if (attacker != null
         && attacker.getPersistentData().getLong(NightingaleSupportService.NP_POWER_DOWN_UNTIL) > target.level().getGameTime()
         && NoblePhantasmDamageClassifier.isNoblePhantasmDamage(event.getSource(), event.getOriginalAmount())) {
         amount *= 0.5F;
      }
      event.setAmount(amount);
      markAggressorAgainstNearbyNightingales(target, attacker);
   }

   @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
   public static void protectSafetyCircleEnd(LivingIncomingDamageEvent event) {
      if (shouldCancelSafetyCircleDamage(event.getEntity(), event.getSource())) {
         cancel(event);
      } else if (event.getSource().is(NightingaleDamageTypes.HEALING_REVERSAL)) {
         event.setCanceled(false);
         event.setAmount(event.getOriginalAmount());
         event.setInvulnerabilityTicks(0);
         for (DamageContainer.Reduction reduction : DamageContainer.Reduction.values()) {
            event.addReductionModifier(reduction, (container, amount) -> 0.0F);
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void rejectHarmfulEffects(MobEffectEvent.Applicable event) {
      if (event.getEffectInstance().getEffect().value().getCategory() == MobEffectCategory.HARMFUL
         && NightingaleSupportService.isProtected(event.getEntity())) {
         event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void rejectKnockback(LivingKnockBackEvent event) {
      if (NightingaleSupportService.isProtected(event.getEntity())) event.setCanceled(true);
   }

   @SubscribeEvent
   public static void tickBuffsAndCircle(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) return;
      long now = living.level().getGameTime();
      NightingaleSupportService.tickBuffs(living, now);
      if (NightingaleSupportService.isProtected(living) && now % 10L == 0L) NightingaleSupportService.cleanse(living);
      if (living instanceof NightingaleEntity nightingale && now % 5L == 0L
         && nightingale.level() instanceof net.minecraft.server.level.ServerLevel level) {
         NightingaleSupportService.tickSafetyCircleVisuals(level, now);
      }
   }

   @SubscribeEvent
   public static void clearCardCastOnLogout(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) ServantCardNightingaleSkills.clear(player, true);
   }

   @SubscribeEvent
   public static void clearCardCastOnDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) ServantCardNightingaleSkills.clear(player, false);
   }

   @SubscribeEvent
   public static void clearCardCastOnDimensionChange(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) ServantCardNightingaleSkills.clear(player, true);
   }

   private static void markAggressorAgainstNearbyNightingales(LivingEntity victim, LivingEntity attacker) {
      if (attacker == null || attacker == victim) return;
      for (NightingaleEntity nightingale : victim.level().getEntitiesOfClass(
         NightingaleEntity.class, victim.getBoundingBox().inflate(32.0), entity -> entity.isAlive() && NightingaleSupportService.isAlly(entity, victim))) {
         nightingale.markAggressor(attacker);
      }
   }

   private static LivingEntity resolveAttacker(DamageSource source) {
      Entity entity = source.getEntity();
      if (entity instanceof LivingEntity living) return living;
      Entity direct = source.getDirectEntity();
      return direct instanceof LivingEntity living ? living : null;
   }

   private static boolean shouldCancelSafetyCircleDamage(LivingEntity target, DamageSource source) {
      return NightingaleSupportService.isProtected(target)
         && !source.is(DamageTypes.FELL_OUT_OF_WORLD)
         && !source.is(DamageTypes.GENERIC_KILL)
         && !source.is(MuramasaDamageTypes.TSUMUKARI_MURAMASA);
   }

   private static void cancel(LivingIncomingDamageEvent event) {
      event.setAmount(0.0F);
      event.setInvulnerabilityTicks(0);
      event.setCanceled(true);
   }
}
