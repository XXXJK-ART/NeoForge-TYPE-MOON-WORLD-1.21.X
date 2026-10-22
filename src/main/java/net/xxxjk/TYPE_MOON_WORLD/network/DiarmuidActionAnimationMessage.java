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

public record DiarmuidActionAnimationMessage(UUID owner, int actionId, int handId) implements CustomPacketPayload {
   /** Natural completion preserves the client's authored recovery; -1 cancels immediately. */
   public static final int FINISH = -2;
   public DiarmuidActionAnimationMessage(UUID owner, int actionId) { this(owner, actionId, -1); }
   public static final Type<DiarmuidActionAnimationMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "diarmuid_action"));
   public static final StreamCodec<RegistryFriendlyByteBuf, DiarmuidActionAnimationMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> { buffer.writeUUID(message.owner); buffer.writeVarInt(message.actionId); buffer.writeByte(message.handId); },
      buffer -> new DiarmuidActionAnimationMessage(buffer.readUUID(), buffer.readVarInt(), buffer.readByte()));
   @Override public Type<DiarmuidActionAnimationMessage> type() { return TYPE; }
   public static void handleData(DiarmuidActionAnimationMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND) return;
      context.enqueueWork(() -> {
         if (FMLEnvironment.dist == Dist.CLIENT) net.xxxjk.TYPE_MOON_WORLD.client.DiarmuidActionAnimationClient.play(message.owner, message.actionId, message.handId);
      });
   }
}
