package net.xxxjk.TYPE_MOON_WORLD.servant.artoria;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.ArtoriaExcaliburAnimationMessage;

public final class ArtoriaExcaliburAnimationService {
   private ArtoriaExcaliburAnimationService() { }

   public static void play(LivingEntity owner, ArtoriaExcaliburAction action) {
      if (owner == null || action == null || owner.level().isClientSide()) return;
      var message = new ArtoriaExcaliburAnimationMessage(owner.getUUID(), action.id(), owner.level().getGameTime());
      PacketDistributor.sendToPlayersTrackingEntity(owner, message);
      if (owner instanceof ServerPlayer player) net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork.sendToPlayer(player, message);
   }
}
