package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.OdaNobunagaActionAnimationClient;
import org.jetbrains.annotations.NotNull;

public record OdaNobunagaActionAnimationMessage(UUID playerId, int actionId) implements CustomPacketPayload {
   public static final Type<OdaNobunagaActionAnimationMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "oda_nobunaga_action_animation")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, OdaNobunagaActionAnimationMessage> STREAM_CODEC = StreamCodec.of(
      OdaNobunagaActionAnimationMessage::write,
      OdaNobunagaActionAnimationMessage::read
   );

   @Override
   @NotNull
   public Type<OdaNobunagaActionAnimationMessage> type() {
      return TYPE;
   }

   private static void write(RegistryFriendlyByteBuf buffer, OdaNobunagaActionAnimationMessage message) {
      buffer.writeUUID(message.playerId);
      buffer.writeVarInt(message.actionId);
   }

   private static OdaNobunagaActionAnimationMessage read(RegistryFriendlyByteBuf buffer) {
      return new OdaNobunagaActionAnimationMessage(buffer.readUUID(), buffer.readVarInt());
   }

   public static void handleData(OdaNobunagaActionAnimationMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND) return;
      context.enqueueWork(() -> {
         if (FMLEnvironment.dist == Dist.CLIENT) {
            OdaNobunagaActionAnimationClient.play(message.playerId, message.actionId);
         }
      });
   }
}
