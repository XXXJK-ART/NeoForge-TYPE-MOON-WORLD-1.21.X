package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardOdaNobunagaSkills;
import org.jetbrains.annotations.NotNull;

public record ServantCardBasicAttackMessage(boolean secondary) implements CustomPacketPayload {
   public static final Type<ServantCardBasicAttackMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_card_basic_attack")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantCardBasicAttackMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeBoolean(message.secondary),
      buffer -> new ServantCardBasicAttackMessage(buffer.readBoolean())
   );

   @Override
   @NotNull
   public Type<ServantCardBasicAttackMessage> type() {
      return TYPE;
   }

   public static void handleData(ServantCardBasicAttackMessage message, IPayloadContext context) {
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            ServantCardOdaNobunagaSkills.handleBasicAttackPacket(player, message.secondary);
         }
      });
   }
}
