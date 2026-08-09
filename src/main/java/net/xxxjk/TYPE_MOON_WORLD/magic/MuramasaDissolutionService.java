package net.xxxjk.TYPE_MOON_WORLD.magic;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Handles the delayed spiritual dissolution caused by overcharging Tsumukari.
 */
public final class MuramasaDissolutionService {
   public static final String DISSOLUTION_UNTIL = "MuramasaTsumukariDissolutionUntil";
   public static final int DISSOLUTION_DELAY_TICKS = 200;

   private MuramasaDissolutionService() {
   }

   public static void schedule(LivingEntity entity) {
      schedule(entity, DISSOLUTION_DELAY_TICKS);
   }

   public static void schedule(LivingEntity entity, int delayTicks) {
      if (entity == null || entity.level().isClientSide()
         || entity instanceof Player player && player.isCreative()) {
         return;
      }
      int clampedDelay = Math.max(1, delayTicks);
      entity.getPersistentData().putLong(
         DISSOLUTION_UNTIL,
         entity.level().getGameTime() + clampedDelay
      );
   }

   public static boolean tick(LivingEntity entity) {
      if (entity == null || entity.level().isClientSide()) {
         return false;
      }
      long until = entity.getPersistentData().getLong(DISSOLUTION_UNTIL);
      if (until <= 0L || entity.level().getGameTime() < until) {
         return false;
      }
      entity.getPersistentData().remove(DISSOLUTION_UNTIL);
      if (entity instanceof Player player && player.isCreative()) {
         return false;
      }
      if (net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper.tryProtectWithAvalon(entity)) {
         return true;
      }
      if (entity.isAlive()) {
         entity.invulnerableTime = 0;
         entity.hurt(entity.damageSources().genericKill(), Float.MAX_VALUE);
      }
      return true;
   }
}
