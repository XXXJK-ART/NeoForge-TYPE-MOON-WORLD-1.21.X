package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BizenNagamitsuItem;

public final class SasakiKojiroCombatHelper {
   public static final String LAST_COMBAT_TICK_TAG = "SasakiKojiroLastCombatTick";
   private static final String LAST_REPAIR_TICK_TAG = "SasakiKojiroLastRepairTick";
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

   private static ItemStack getBladeStack(ServantEntity entity) {
      ItemStack mainHand = entity.getMainHandItem();
      if (!mainHand.isEmpty() && mainHand.getItem() instanceof BizenNagamitsuItem && mainHand.isDamageableItem()) {
         return mainHand;
      }
      return ItemStack.EMPTY;
   }
}
