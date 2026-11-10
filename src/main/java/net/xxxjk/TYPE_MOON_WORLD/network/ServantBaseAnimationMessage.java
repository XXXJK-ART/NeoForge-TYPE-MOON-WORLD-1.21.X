package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantBaseAnimationClient;

public record ServantBaseAnimationMessage(UUID entityId, int action) implements CustomPacketPayload {
   public static final Type<ServantBaseAnimationMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_base_animation"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantBaseAnimationMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> { buf.writeUUID(msg.entityId); buf.writeByte(msg.action); },
      buf -> new ServantBaseAnimationMessage(buf.readUUID(), buf.readByte()));

   @Override public Type<ServantBaseAnimationMessage> type() { return TYPE; }

   public static void handleData(ServantBaseAnimationMessage msg, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND) return;
      context.enqueueWork(() -> ServantBaseAnimationClient.play(msg.entityId, msg.action));
   }
}
