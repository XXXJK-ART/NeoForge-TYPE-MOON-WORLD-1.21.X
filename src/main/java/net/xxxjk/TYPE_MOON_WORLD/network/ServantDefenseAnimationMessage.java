package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantDefenseAnimationClient;

public record ServantDefenseAnimationMessage(UUID entityId, int animation) implements CustomPacketPayload {
   public static final Type<ServantDefenseAnimationMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_defense_animation"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantDefenseAnimationMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> { buf.writeUUID(msg.entityId); buf.writeByte(msg.animation); },
      buf -> new ServantDefenseAnimationMessage(buf.readUUID(), buf.readByte()));

   @Override public Type<ServantDefenseAnimationMessage> type() { return TYPE; }

   public static void handleData(ServantDefenseAnimationMessage msg, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND) return;
      context.enqueueWork(() -> {
         if (msg.animation == ServantDefenseAnimationClient.ACTIVE) {
            ServantDefenseAnimationClient.applyGuarding(msg.entityId, true);
         } else if (msg.animation == ServantDefenseAnimationClient.PASSIVE) {
            ServantDefenseAnimationClient.triggerPassive(msg.entityId);
         } else {
            ServantDefenseAnimationClient.applyGuarding(msg.entityId, false);
         }
      });
   }
}
