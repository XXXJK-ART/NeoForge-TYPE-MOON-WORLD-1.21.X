package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BizenNagamitsuItem;

public final class SasakiKojiroCombatHelper {
   public static final String MINDSEYE_ACTIVE_TAG = "MindseyeActive";
   public static final String MINDSEYE_DODGE_CHANCE_TAG = "MindseyeDodgeChance";
   public static final String MINDSEYE_BLOCK_CHANCE_TAG = "MindseyeBlockChance";
   public static final String LAST_COMBAT_TICK_TAG = "SasakiKojiroLastCombatTick";
   private static final String LAST_REPAIR_TICK_TAG = "SasakiKojiroLastRepairTick";
   private static final float FULL_ACCURACY_DURABILITY_RATIO = 0.70F;
   private static final float MIN_TSURIGAMESHI_ACCURACY = 0.70F;
   private static final int REPAIR_DELAY_TICKS = 100;
   private static final int REPAIR_INTERVAL_TICKS = 20;
   private static final int REPAIR_AMOUNT = 5;

   private SasakiKojiroCombatHelper() {
   }

   public static boolean isSasakiKojiro(ServantEntity entity) {
      return entity instanceof SasakiKojiroEntity;
   }

   public static void markCombat(ServantEntity entity) {
      if (isSasakiKojiro(entity)) {
         entity.getPersistentData().putLong(LAST_COMBAT_TICK_TAG, entity.level().getGameTime());
      }
   }

   public static boolean hasBlade(ServantEntity entity) {
      return !getBladeStack(entity).isEmpty();
   }

   public static boolean isBladeBroken(ServantEntity entity) {
      ItemStack blade = getBladeStack(entity);
      return blade.isEmpty() || blade.getMaxDamage() <= 0 || blade.getDamageValue() >= blade.getMaxDamage();
   }

   public static float getTsurigameshiHitChance(ServantEntity entity) {
      ItemStack blade = getBladeStack(entity);
      if (blade.isEmpty() || blade.getMaxDamage() <= 0) {
         return 0.0F;
      }
      if (isBladeBroken(entity)) {
         return 0.0F;
      }

      float remainingRatio = getRemainingDurabilityRatio(blade);
      if (remainingRatio >= FULL_ACCURACY_DURABILITY_RATIO) {
         return 1.0F;
      }

      float scaledRatio = remainingRatio / FULL_ACCURACY_DURABILITY_RATIO;
      return Math.max(MIN_TSURIGAMESHI_ACCURACY, MIN_TSURIGAMESHI_ACCURACY + scaledRatio * (1.0F - MIN_TSURIGAMESHI_ACCURACY));
   }

   public static int damageBladeFromIncomingAttack(ServantEntity entity, float incomingDamage) {
      ItemStack blade = getBladeStack(entity);
      if (blade.isEmpty() || blade.getMaxDamage() <= 0) {
         return 0;
      }

      int durabilityLoss = Math.max(1, Mth.ceil(incomingDamage));
      int newDamage = Math.min(blade.getMaxDamage(), blade.getDamageValue() + durabilityLoss);
      blade.setDamageValue(newDamage);
      markCombat(entity);
      return durabilityLoss;
   }

   public static void repairBladeOutOfCombat(ServantEntity entity) {
      if (!isSasakiKojiro(entity)) {
         return;
      }

      ItemStack blade = getBladeStack(entity);
      if (blade.isEmpty() || blade.getDamageValue() <= 0) {
         return;
      }

      long gameTime = entity.level().getGameTime();
      long lastCombatTick = entity.getPersistentData().getLong(LAST_COMBAT_TICK_TAG);
      if (gameTime - lastCombatTick < REPAIR_DELAY_TICKS) {
         return;
      }

      long lastRepairTick = entity.getPersistentData().getLong(LAST_REPAIR_TICK_TAG);
      if (gameTime - lastRepairTick < REPAIR_INTERVAL_TICKS) {
         return;
      }

      blade.setDamageValue(Math.max(0, blade.getDamageValue() - REPAIR_AMOUNT));
      entity.getPersistentData().putLong(LAST_REPAIR_TICK_TAG, gameTime);
   }

   private static float getRemainingDurabilityRatio(ItemStack blade) {
      if (blade.getMaxDamage() <= 0) {
         return 1.0F;
      }
      int remaining = Math.max(0, blade.getMaxDamage() - blade.getDamageValue());
      return remaining / (float) blade.getMaxDamage();
   }

   private static ItemStack getBladeStack(ServantEntity entity) {
      ItemStack mainHand = entity.getMainHandItem();
      if (!mainHand.isEmpty() && mainHand.getItem() instanceof BizenNagamitsuItem && mainHand.isDamageableItem()) {
         return mainHand;
      }
      return ItemStack.EMPTY;
   }
}
