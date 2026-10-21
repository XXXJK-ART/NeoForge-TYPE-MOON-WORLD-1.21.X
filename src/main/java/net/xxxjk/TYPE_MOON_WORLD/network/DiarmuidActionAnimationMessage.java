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

public record DiarmuidActionAnimationMessage(UUID owner, int actionId) implements CustomPacketPayload {
   public static final Type<DiarmuidActionAnimationMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "diarmuid_action"));
   public static final StreamCodec<RegistryFriendlyByteBuf, DiarmuidActionAnimationMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> { buffer.writeUUID(message.owner); buffer.writeVarInt(message.actionId); },
      buffer -> new DiarmuidActionAnimationMessage(buffer.readUUID(), buffer.readVarInt()));
   @Override public Type<DiarmuidActionAnimationMessage> type() { return TYPE; }
   public static void handleData(DiarmuidActionAnimationMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND) return;
      context.enqueueWork(() -> {
         if (FMLEnvironment.dist == Dist.CLIENT) net.xxxjk.TYPE_MOON_WORLD.client.DiarmuidActionAnimationClient.play(message.owner, message.actionId);
      });
   }
}
