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
import net.xxxjk.TYPE_MOON_WORLD.client.SasakiKojiroActionAnimationClient;
import org.jetbrains.annotations.NotNull;

public record SasakiKojiroActionAnimationMessage(UUID playerId, int actionId) implements CustomPacketPayload {
   public static final Type<SasakiKojiroActionAnimationMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "sasaki_kojiro_action_animation"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SasakiKojiroActionAnimationMessage> STREAM_CODEC = StreamCodec.of(
      SasakiKojiroActionAnimationMessage::write, SasakiKojiroActionAnimationMessage::read);

   @Override @NotNull public Type<SasakiKojiroActionAnimationMessage> type() { return TYPE; }

   private static void write(RegistryFriendlyByteBuf buffer, SasakiKojiroActionAnimationMessage message) {
      buffer.writeUUID(message.playerId);
      buffer.writeVarInt(message.actionId);
   }

   private static SasakiKojiroActionAnimationMessage read(RegistryFriendlyByteBuf buffer) {
      return new SasakiKojiroActionAnimationMessage(buffer.readUUID(), buffer.readVarInt());
   }

   public static void handleData(SasakiKojiroActionAnimationMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND) return;
      context.enqueueWork(() -> {
         if (FMLEnvironment.dist == Dist.CLIENT) SasakiKojiroActionAnimationClient.play(message.playerId, message.actionId);
      });
   }
}
