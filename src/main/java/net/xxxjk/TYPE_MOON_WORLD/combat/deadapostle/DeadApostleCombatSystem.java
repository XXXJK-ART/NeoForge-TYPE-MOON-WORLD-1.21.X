package net.xxxjk.TYPE_MOON_WORLD.combat.deadapostle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosBeastLogic;

/**
 * Independent combat layer for high-tier dead apostles.
 *
 * <p>Do not add the existing low-tier dead apostle classes here. They
 * deliberately keep their simple vanilla combat behavior.</p>
 */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class DeadApostleCombatSystem {
   public static final String TAG_STAMINA = "DeadApostleCombatStamina";
   public static final String TAG_POISE = "DeadApostleCombatPoise";
   public static final String TAG_LAST_DODGE = "DeadApostleCombatLastDodge";
   public static final String TAG_LAST_GUARD = "DeadApostleCombatLastGuard";
   public static final String TAG_GUARD_EXHAUST = "DeadApostleCombatGuardExhaust";
   public static final String TAG_INVULN_UNTIL = "DeadApostleCombatInvulnerableUntil";
   public static final String TAG_STUN_UNTIL = "DeadApostleCombatStunUntil";
   public static final String TAG_GUARD_BROKEN = "DeadApostleCombatGuardBroken";

   private DeadApostleCombatSystem() {
   }

   public static DeadApostleCombatProfile profile(String id) {
      return DeadApostleCombatProfileLoader.get(id);
   }

   public static boolean tick(NeroChaosEntity entity) {
      DeadApostleCombatProfile profile = profile(entity.getCombatProfileId());
      var data = entity.getPersistentData();
      long now = entity.level().getGameTime();
      if (!data.contains(TAG_STAMINA)) data.putDouble(TAG_STAMINA, profile.staminaMax());
      if (!data.contains(TAG_POISE)) data.putDouble(TAG_POISE, profile.poiseMax());
      if (entity.tickCount % 5 == 0) {
         if (now >= data.getLong(TAG_GUARD_EXHAUST)) {
            data.putDouble(TAG_STAMINA, Math.min(profile.staminaMax(),
               data.getDouble(TAG_STAMINA) + profile.staminaRegenPerSecond() / 4.0));
         }
         data.putDouble(TAG_POISE, Math.min(profile.poiseMax(),
            data.getDouble(TAG_POISE) + profile.poiseRegenPerSecond() / 4.0));
      }

      AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null) {
         double target = profile.movementSpeed() * (entity.isChaosForm() ? 1.5 : 1.0);
         speed.setBaseValue(target);
      }
      if (data.getLong(TAG_STUN_UNTIL) > now && !entity.isChaosForm()) {
         entity.getNavigation().stop();
         entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.25, 1.0, 0.25));
         return true;
      }
      data.remove(TAG_GUARD_BROKEN);
      return false;
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (!(event.getEntity() instanceof NeroChaosEntity nero)
         || event.isCanceled() || nero.level().isClientSide()) return;

      DamageSource source = event.getSource();
      if (isForcedDeath(source)) return;
      long now = nero.level().getGameTime();
      var data = nero.getPersistentData();
      if (now < data.getLong(TAG_INVULN_UNTIL)) {
         event.setCanceled(true);
         return;
      }
      DeadApostleCombatProfile profile = profile(nero.getCombatProfileId());
      boolean urgent = event.getAmount() >= nero.getMaxHealth() * 0.08F
         || source.getDirectEntity() instanceof Projectile
         || source.is(DamageTypeTags.IS_EXPLOSION)
         || nero.getHealth() <= nero.getMaxHealth() * 0.55F;

      double chance = urgent ? profile.urgentDodgeChance() : profile.dodgeChance();
      if (!nero.isChaosForm()
         && canReact(nero, source)
         && now >= data.getLong(TAG_LAST_DODGE) + profile.dodgeCooldownTicks()
         && nero.getRandom().nextDouble() < chance) {
         data.putLong(TAG_LAST_DODGE, now);
         data.putLong(TAG_INVULN_UNTIL, now + profile.dodgeInvulnerabilityTicks());
         Vec3 away = dodgeDirection(nero, source);
         nero.setDeltaMovement(away.x * 0.95, Math.max(nero.getDeltaMovement().y, 0.08), away.z * 0.95);
         nero.hurtMarked = true;
         if (nero.level() instanceof net.minecraft.server.level.ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, nero.getX(), nero.getY() + 0.9, nero.getZ(),
               8, 0.25, 0.35, 0.25, 0.03);
         }
         event.setCanceled(true);
         return;
      }

      if (!nero.isChaosForm()
         && canReact(nero, source)
         && now >= data.getLong(TAG_GUARD_EXHAUST)
         && data.getDouble(TAG_STAMINA) >= profile.blockStaminaCost()) {
         data.putDouble(TAG_STAMINA,
            Math.max(0.0, data.getDouble(TAG_STAMINA) - profile.blockStaminaCost()));
         data.putLong(TAG_LAST_GUARD, now);
         event.setAmount(event.getAmount() * (float)(1.0 - profile.blockReduction()));
      }

      if (!event.isCanceled() && !nero.isChaosForm()
         && event.getAmount() >= nero.getMaxHealth() * 0.08F) {
         double poise = Math.max(0.0, data.getDouble(TAG_POISE) - profile.poiseDamagePerHit());
         data.putDouble(TAG_POISE, poise);
         if (poise <= 0.0) {
            data.putLong(TAG_STUN_UNTIL, now + profile.guardBreakTicks());
            data.putDouble(TAG_POISE, profile.poiseMax() * 0.5);
            data.putBoolean(TAG_GUARD_BROKEN, true);
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onLivingDeath(LivingDeathEvent event) {
      if (event.isCanceled()) return;
      if (event.getEntity() instanceof NeroChaosEntity nero) {
         if (isForcedDeath(event.getSource())) return;
         int lives = nero.getRemainingLives();
         if (NeroChaosRules.shouldReviveAfterLethal(lives, false)) {
            nero.setRemainingLives(NeroChaosRules.consumeLife(lives));
            nero.reviveFromDeath();
            event.setCanceled(true);
         } else {
            nero.setRemainingLives(0);
         }
      } else if (event.getEntity() instanceof LivingEntity beast
         && NeroChaosBeastLogic.isBeast(beast)) {
         NeroChaosBeastLogic.onDeath(beast);
      }
   }

   public static boolean isForcedDeath(DamageSource source) {
      return source.is(DamageTypes.GENERIC_KILL)
         || source.is(DamageTypes.FELL_OUT_OF_WORLD)
         || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
   }

   private static boolean canReact(NeroChaosEntity entity, DamageSource source) {
      if (source == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
      Entity direct = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
      return direct != entity;
   }

   private static Vec3 dodgeDirection(NeroChaosEntity entity, DamageSource source) {
      Entity sourceEntity = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
      Vec3 away;
      if (sourceEntity instanceof Projectile projectile
         && projectile.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
         away = projectile.getDeltaMovement().multiply(-1.0, 0.0, -1.0);
      } else if (sourceEntity != null) {
         away = entity.position().subtract(sourceEntity.position()).multiply(1.0, 0.0, 1.0);
      } else {
         away = new Vec3(entity.getRandom().nextDouble() - 0.5, 0.0, entity.getRandom().nextDouble() - 0.5);
      }
      return away.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
   }
}
