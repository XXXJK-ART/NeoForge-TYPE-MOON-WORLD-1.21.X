package net.xxxjk.TYPE_MOON_WORLD.magic.player;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

public final class MercurySwordMagicAmplifier {
   public static final float DAMAGE_MULTIPLIER = 1.25F;
   public static final float DURATION_MULTIPLIER = 1.2F;
   public static final double RADIUS_FLAT_BONUS = 1.5;
   public static final double RADIUS_MAX_MULTIPLIER = 1.25;
   public static final double COOLDOWN_MULTIPLIER = 0.85;

   private MercurySwordMagicAmplifier() {
   }

   public static boolean isHolding(LivingEntity entity) {
      if (entity == null) {
         return false;
      }
      return isMercurySword(entity.getMainHandItem()) || isMercurySword(entity.getOffhandItem());
   }

   public static boolean isMercurySword(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.is(ModItems.MERCURY_SWORD.get());
   }

   public static float amplifyDamage(LivingEntity entity, float damage) {
      return isHolding(entity) ? damage * DAMAGE_MULTIPLIER : damage;
   }

   public static double amplifyRadius(LivingEntity entity, double radius) {
      if (!isHolding(entity)) {
         return radius;
      }
      return Math.min(radius * RADIUS_MAX_MULTIPLIER, radius + RADIUS_FLAT_BONUS);
   }

   public static int amplifyDuration(LivingEntity entity, int ticks) {
      return isHolding(entity) ? Math.max(1, Math.round(ticks * DURATION_MULTIPLIER)) : ticks;
   }

   public static double amplifyCooldown(LivingEntity entity, double cooldown) {
      return isHolding(entity) ? Math.max(1.0, cooldown * COOLDOWN_MULTIPLIER) : cooldown;
   }
}
