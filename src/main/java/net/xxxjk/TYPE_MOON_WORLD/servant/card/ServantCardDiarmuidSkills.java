package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAbilityService;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidCombatHelper;

/** Input adaptation only; shared service owns damage, mana, cooldowns and animation. */
public final class ServantCardDiarmuidSkills {
   public static final int DUAL_WIELD_HAND_ATTACK_COOLDOWN_TICKS = DiarmuidAction.HAND_COOLDOWN;
   private ServantCardDiarmuidSkills() { }

   public static boolean isActiveCard(ServerPlayer player) {
      var card = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
      return card.transformed() && "diarmuid_ua_duibhne".equals(card.id());
   }

   public static void initialize(ServerPlayer player) {
      DiarmuidCombatHelper.ensureDurability(player);
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_action_mode = 1;
      removeOldStrategy(player);
      vars.syncPlayerVariables(player);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!isActiveCard(player)) { DiarmuidAbilityService.cancel(player); return; }
      if (vars.servant_card_action_mode != 1) {
         vars.servant_card_action_mode = 1;
         vars.syncPlayerVariables(player);
      }
      removeOldStrategy(player);
   }

   public static void clear(ServerPlayer player) {
      DiarmuidAbilityService.cancel(player);
      if (player.getPersistentData().contains(DiarmuidCombatHelper.YELLOW_SPEAR_DURABILITY_TAG)) DiarmuidCombatHelper.clearCursesFromOwner(player);
      removeOldStrategy(player);
      player.getPersistentData().remove(DiarmuidCombatHelper.RED_SPEAR_DURABILITY_TAG);
      player.getPersistentData().remove(DiarmuidCombatHelper.YELLOW_SPEAR_DURABILITY_TAG);
   }

   private static void removeOldStrategy(ServerPlayer player) {
      player.getPersistentData().remove("ServantCardDiarmuidStrategyUntil");
      player.getPersistentData().remove("ServantCardDiarmuidFocusSpear");
      player.getPersistentData().remove("ServantCardDiarmuidFocusUntil");
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.ATTACK_SPEED),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_card_diarmuid_strategy_attack_speed"));
      ServantCardSkillUtils.remove(player.getAttribute(Attributes.MOVEMENT_SPEED),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_card_diarmuid_strategy_move_speed"));
   }

   public static boolean isDualWieldActive(ServerPlayer player) { return isActiveCard(player); }
   public static void performOffhandSpearThrust(ServerPlayer player) { DiarmuidAbilityService.basic(player, InteractionHand.OFF_HAND); }
   public static void performMainhandSpearThrust(ServerPlayer player) { DiarmuidAbilityService.basic(player, InteractionHand.MAIN_HAND); }
}
