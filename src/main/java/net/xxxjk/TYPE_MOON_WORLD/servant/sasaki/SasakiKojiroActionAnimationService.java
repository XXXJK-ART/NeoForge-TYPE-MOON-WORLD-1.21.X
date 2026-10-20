package net.xxxjk.TYPE_MOON_WORLD.servant.sasaki;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.SasakiKojiroActionAnimationMessage;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroEntity;

public final class SasakiKojiroActionAnimationService {
   private SasakiKojiroActionAnimationService() { }

   public static void play(LivingEntity owner, SasakiKojiroAction action) {
      if (owner == null || action == null || owner.level().isClientSide()) return;
      if (owner instanceof SasakiKojiroEntity sasaki) {
         sasaki.triggerSasakiAction(action);
      }
      if (owner instanceof ServerPlayer player) {
         var message = new SasakiKojiroActionAnimationMessage(player.getUUID(), action.networkId());
         PacketDistributor.sendToPlayersTrackingEntity(player, message);
         net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork.sendToPlayer(player, message);
      } else if (action == SasakiKojiroAction.TSUBAME_GAESHI) {
         PacketDistributor.sendToPlayersTrackingEntity(owner,
            new SasakiKojiroActionAnimationMessage(owner.getUUID(), action.networkId()));
      }
   }
}
