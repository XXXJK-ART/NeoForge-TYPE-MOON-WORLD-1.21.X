package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;

public final class HeraclesGodHandHelper {
   private static final String CYBELE_ADAPTED_TAG = "GodHandAdaptedMedusaCybele";
   private static final String ZABANIYA_ADAPTED_TAG = "GodHandAdaptedCursedArmZabaniya";
   private static final float ANTI_HERACLES_NOBLE_PHANTASM_MULTIPLIER = 1.5F;

   private HeraclesGodHandHelper() {
   }

   public static boolean hasGodHand(LivingEntity target) {
      if (!target.getPersistentData().getBoolean("GodHandActive")) {
         return false;
      }
      if (target instanceof HeraclesEntity) {
         return true;
      }
      ServantDefinition definition = ServantIdentityHelper.definitionOf(target);
      return definition != null && "heracles".equals(definition.id());
   }

   public static float applyAntiHeraclesNoblePhantasmSpecialAttack(LivingEntity target, float damage) {
      return hasGodHand(target) ? damage * ANTI_HERACLES_NOBLE_PHANTASM_MULTIPLIER : damage;
   }

   public static boolean isAdaptedToCybele(LivingEntity target) {
      return hasGodHand(target) && target.getPersistentData().getBoolean(CYBELE_ADAPTED_TAG);
   }

   public static boolean isAdaptedToZabaniya(LivingEntity target) {
      return hasGodHand(target) && target.getPersistentData().getBoolean(ZABANIYA_ADAPTED_TAG);
   }

   /** Returns whether a lethal hit just consumed one God Hand life and revived the target. */
   public static boolean consumedLifeAfterLethalHit(LivingEntity target, int livesBeforeHit) {
      if (target == null || livesBeforeHit <= 0) {
         return false;
      }
      CompoundTag data = target.getPersistentData();
      return !data.getBoolean("CausalSevered")
         && data.getBoolean("GodHandActive")
         && data.getInt("GodHandLives") == livesBeforeHit - 1;
   }

   public static boolean consumeLifeForCybele(LivingEntity target) {
      return consumeLife(target, CYBELE_ADAPTED_TAG);
   }

   public static boolean consumeLifeForZabaniya(LivingEntity target) {
      return consumeLife(target, ZABANIYA_ADAPTED_TAG);
   }

   public static void applyAdaptedSlow(LivingEntity target, int durationTicks) {
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 1, false, true, true));
   }

   private static boolean consumeLife(LivingEntity target, String adaptedTag) {
      if (!hasGodHand(target)) {
         return false;
      }

      CompoundTag data = target.getPersistentData();
      if (data.getBoolean(adaptedTag)) {
         return false;
      }

      int livesLeft = data.getInt("GodHandLives");
      if (livesLeft <= 0) {
         return false;
      }

      target.setHealth(target.getMaxHealth());
      data.putInt("GodHandLives", livesLeft - 1);
      data.putBoolean(adaptedTag, true);
      if (target.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY() + 1.0, target.getZ(), 30, 0.6, 0.6, 0.6, 0.15);
         level.sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + 0.5, target.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
         level.playSound(null, target.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.0F, 0.8F);
      }
      return true;
   }
}
