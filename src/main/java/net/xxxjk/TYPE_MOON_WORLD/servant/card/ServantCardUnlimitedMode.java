package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.GilgameshDivineShield;

public final class ServantCardUnlimitedMode {
   private static final String TAG_ENABLED = "TypeMoonServantCardUnlimited";
   private static final String[] CUSTOM_COOLDOWN_TAGS = {
      "ServantCardGilgameshSingleVaultCooldown",
      "ServantCardGilgameshMeleeCooldown",
      "ServantCardOdaPrimaryAttackCooldown",
      "ServantCardOdaSecondaryAttackCooldown"
   };

   private ServantCardUnlimitedMode() {
   }

   public static boolean isEnabled(ServerPlayer player) {
      if (player == null) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && player.getPersistentData().getBoolean(TAG_ENABLED);
   }

   public static void setEnabled(ServerPlayer player, boolean enabled) {
      player.getPersistentData().putBoolean(TAG_ENABLED, enabled);
      if (enabled) {
         clearCooldowns(player);
      }
   }

   public static void clearCooldowns(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_skill_cooldowns = "";
      vars.servant_card_skill_cooldown_ends = "";
      vars.servant_card_np_cooldown = 0;
      vars.servant_card_np_cooldown_end = 0L;
      vars.servant_card_flight_toggle_cooldown = 0;
      vars.servant_card_high_flight_cooldown_until = 0L;
      vars.servant_card_oda_flight_cooldown_until = 0L;
      vars.servant_card_oda_flight_recharge_at = 0L;
      vars.servant_card_oda_flight_ticks = 100;
      for (String tag : CUSTOM_COOLDOWN_TAGS) {
         player.getPersistentData().remove(tag);
      }
      GilgameshDivineShield.clearCooldown(player);
      clearItemCooldowns(player);
   }

   /** Clears cooldowns attached to held or inventory items, including projected Noble Phantasms. */
   private static void clearItemCooldowns(ServerPlayer player) {
      for (ItemStack stack : player.getInventory().items) {
         clearItemCooldown(player, stack);
      }
      for (ItemStack stack : player.getInventory().armor) {
         clearItemCooldown(player, stack);
      }
      for (ItemStack stack : player.getInventory().offhand) {
         clearItemCooldown(player, stack);
      }
   }

   private static void clearItemCooldown(ServerPlayer player, ItemStack stack) {
      if (!stack.isEmpty()) {
         player.getCooldowns().removeCooldown(stack.getItem());
      }
   }
}
