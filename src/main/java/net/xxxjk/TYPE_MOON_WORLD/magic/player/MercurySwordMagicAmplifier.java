package net.xxxjk.TYPE_MOON_WORLD.magic.player;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;

public final class MercurySwordMagicAmplifier {
   private static final int ELEMENT_FIRE = 0;
   public static final float DAMAGE_MULTIPLIER = 1.5F;
   public static final float RUBY_FOCUS_DAMAGE_MULTIPLIER = 1.3F;
   public static final float RUBY_FOCUS_FIRE_DAMAGE_MULTIPLIER = 1.25F;
   public static final float DURATION_MULTIPLIER = 1.4F;
   public static final double RADIUS_FLAT_BONUS = 2.5;
   public static final double RADIUS_MAX_MULTIPLIER = 1.5;
   public static final double COOLDOWN_MULTIPLIER = 0.75;
   public static final double SAPPHIRE_MANA_MULTIPLIER = 0.65;
   public static final double CYAN_RANGE_MULTIPLIER = 1.5;
   public static final double CYAN_RANGE_FLAT_BONUS = 3.0;
   public static final double CYAN_COOLDOWN_MULTIPLIER = 0.7;
   public static final double EMERALD_DEFENSE_MULTIPLIER = 1.5;
   public static final double TOPAZ_TERRAIN_MULTIPLIER = 1.5;

   private MercurySwordMagicAmplifier() {
   }

   public static boolean isHolding(LivingEntity entity) {
      if (entity == null) {
         return false;
      }
      return isMercurySword(entity.getMainHandItem()) || isMercurySword(entity.getOffhandItem())
         || isStaff(entity.getMainHandItem()) || isStaff(entity.getOffhandItem());
   }

   public static boolean isMercurySword(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.is(ModItems.MERCURY_SWORD.get());
   }

   public static boolean isRubyStaff(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.is(ModItems.RUBY_STAFF.get());
   }

   private static boolean isStaff(ItemStack stack) {
      return stack != null && !stack.isEmpty()
         && (stack.is(ModItems.STAFF.get()) || stack.is(ModItems.RUBY_STAFF.get())
            || stack.is(ModItems.SAPPHIRE_STAFF.get()) || stack.is(ModItems.EMERALD_STAFF.get())
            || stack.is(ModItems.CYAN_STAFF.get()) || stack.is(ModItems.TOPAZ_STAFF.get()));
   }

   public static boolean isHoldingRubyStaff(LivingEntity entity) {
      if (entity == null) {
         return false;
      }
      return isRubyStaff(entity.getMainHandItem()) || isRubyStaff(entity.getOffhandItem());
   }

   public static float amplifyDamage(LivingEntity entity, float damage) {
      float amplified = isHolding(entity) ? damage * DAMAGE_MULTIPLIER : damage;
      if (isHoldingGemFocus(entity, GemType.RUBY)) {
         amplified *= RUBY_FOCUS_DAMAGE_MULTIPLIER;
      }
      return amplified;
   }

   public static float amplifyElementalDamage(LivingEntity entity, int element, float damage) {
      float amplified = amplifyDamage(entity, damage);
      if (isFireElement(element) && isHoldingGemFocus(entity, GemType.RUBY)) {
         amplified *= RUBY_FOCUS_FIRE_DAMAGE_MULTIPLIER;
      }
      return amplified;
   }

   private static boolean isFireElement(int element) {
      return element == ELEMENT_FIRE;
   }

   public static double amplifyRadius(LivingEntity entity, double radius) {
      double amplified = radius;
      if (isHolding(entity)) {
         amplified = Math.min(amplified * RADIUS_MAX_MULTIPLIER, amplified + RADIUS_FLAT_BONUS);
      }
      if (isHoldingGemFocus(entity, GemType.CYAN)) {
         amplified = Math.min(amplified * CYAN_RANGE_MULTIPLIER, amplified + CYAN_RANGE_FLAT_BONUS);
      }
      return amplified;
   }

   public static int amplifyDuration(LivingEntity entity, int ticks) {
      return isHolding(entity) ? Math.max(1, Math.round(ticks * DURATION_MULTIPLIER)) : ticks;
   }

   public static double amplifyCooldown(LivingEntity entity, double cooldown) {
      double amplified = isHolding(entity) ? Math.max(1.0, cooldown * COOLDOWN_MULTIPLIER) : cooldown;
      if (isHoldingGemFocus(entity, GemType.CYAN)) {
         amplified = Math.max(1.0, amplified * CYAN_COOLDOWN_MULTIPLIER);
      }
      return amplified;
   }

   public static double manaCostMultiplier(LivingEntity entity) {
      return isHoldingGemFocus(entity, GemType.SAPPHIRE) ? SAPPHIRE_MANA_MULTIPLIER : 1.0;
   }

   public static double defenseMultiplier(LivingEntity entity) {
      return isHoldingGemFocus(entity, GemType.EMERALD) ? EMERALD_DEFENSE_MULTIPLIER : 1.0;
   }

   public static double terrainMultiplier(LivingEntity entity) {
      return isHoldingGemFocus(entity, GemType.TOPAZ) ? TOPAZ_TERRAIN_MULTIPLIER : 1.0;
   }

   public static boolean isHoldingGemFocus(LivingEntity entity, GemType type) {
      if (entity == null || type == null) {
         return false;
      }
      return isGemFocus(entity.getMainHandItem(), type) || isGemFocus(entity.getOffhandItem(), type);
   }

   public static boolean isGemFocus(ItemStack stack, GemType type) {
      if (stack == null || stack.isEmpty() || type == null) {
         return false;
      }
      return switch (type) {
         case RUBY -> stack.is(ModItems.LARGE_RUBY.get()) || stack.is(ModItems.RUBY_STAFF.get());
         case SAPPHIRE -> stack.is(ModItems.LARGE_SAPPHIRE.get()) || stack.is(ModItems.SAPPHIRE_STAFF.get());
         case EMERALD -> stack.is(ModItems.LARGE_EMERALD.get()) || stack.is(ModItems.EMERALD_STAFF.get());
         case CYAN -> stack.is(ModItems.LARGE_CYAN_GEMSTONE.get()) || stack.is(ModItems.CYAN_STAFF.get());
         case TOPAZ -> stack.is(ModItems.LARGE_TOPAZ.get()) || stack.is(ModItems.TOPAZ_STAFF.get());
         default -> false;
      };
   }
}
