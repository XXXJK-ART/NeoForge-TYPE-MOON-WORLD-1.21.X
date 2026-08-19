package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardFlightController;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.jetbrains.annotations.NotNull;

public record ServantCardJumpMessage(float forward, float strafe) implements CustomPacketPayload {
   public static final Type<ServantCardJumpMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_card_jump")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantCardJumpMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeFloat(message.forward);
         buffer.writeFloat(message.strafe);
      },
      buffer -> new ServantCardJumpMessage(buffer.readFloat(), buffer.readFloat())
   );

   @Override
   @NotNull
   public Type<ServantCardJumpMessage> type() {
      return TYPE;
   }

   public static void handleData(ServantCardJumpMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND
         || !Float.isFinite(message.forward) || !Float.isFinite(message.strafe)) return;
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player
            && ServerPacketRateLimiter.allow(player, "servant_card_jump", 4)) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (vars.servant_card_transformed && vars.servant_card_flying) {
               ServantCardFlightController.tryDash(player, message.forward, message.strafe);
            } else {
               ServantCardTransformManager.bigJump(player, message.forward, message.strafe);
            }
         }
      });
   }
}
