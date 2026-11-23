package net.xxxjk.TYPE_MOON_WORLD.servant.oda;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import net.xxxjk.TYPE_MOON_WORLD.network.OdaNobunagaActionAnimationMessage;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class OdaNobunagaActionAnimations {
   public enum Action {
      ATTACK_SHOT(1, "attack_shot"),
      ATTACK_SLASH(2, "attack_slash"),
      ATTACK_VOLLEY(3, "attack_volley"),
      SKILL_COMMAND(4, "skill_command"),
      SKILL_MAOU(5, "skill_maou"),
      CHARGED_SHOT(6, "charged_shot"),
      NP_THREE_THOUSAND_CHANT(7, "np_three_thousand_chant"),
      NP_THREE_THOUSAND_RELEASE(8, "np_three_thousand_release"),
      NP_HAJUN_CHANT(9, "np_hajun_chant"),
      NP_HAJUN_RELEASE(10, "np_hajun_release");

      private final int networkId;
      private final String animationId;

      Action(int networkId, String animationId) {
         this.networkId = networkId;
         this.animationId = animationId;
      }

      public int networkId() {
         return this.networkId;
      }

      public String animationId() {
         return this.animationId;
      }

      public String playerAnimationPath() {
         return "oda_" + this.animationId;
      }

      public static Action byNetworkId(int networkId) {
         for (Action action : values()) {
            if (action.networkId == networkId) return action;
         }
         return null;
      }
   }

   private OdaNobunagaActionAnimations() {
   }

   public static void playSkill(LivingEntity owner, int slot) {
      Action action = switch (slot) {
         case -1, 6 -> Action.ATTACK_SLASH;
         case 0, 2, 4, 5 -> Action.SKILL_COMMAND;
         case 1 -> Action.ATTACK_VOLLEY;
         case 3 -> Action.CHARGED_SHOT;
         case 7 -> Action.SKILL_MAOU;
         default -> null;
      };
      play(owner, action);
   }

   public static void play(LivingEntity owner, Action action) {
      if (owner == null || action == null) return;
      if (owner instanceof OdaNobunagaEntity servant) {
         servant.triggerNamedActionAnimation(action.animationId());
         return;
      }
      if (!(owner instanceof ServerPlayer player) || !isOdaPlayerCard(player) || player.level().isClientSide()) return;
      var payload = new OdaNobunagaActionAnimationMessage(player.getUUID(), action.networkId);
      ModNetwork.sendToPlayer(player, payload);
      if (player.level() instanceof ServerLevel level) {
         // Check each recipient independently: GameTest and compatibility players may
         // be present without a client that negotiated this animation channel.
         for (ServerPlayer observer : level.getChunkSource().chunkMap.getPlayersWatching(player)) {
            if (observer != player) ModNetwork.sendToPlayer(observer, payload);
         }
      }
   }

   private static boolean isOdaPlayerCard(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "oda_nobunaga".equals(vars.servant_card_id);
   }
}
