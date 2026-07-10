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
import net.xxxjk.TYPE_MOON_WORLD.client.FirearmPoseClient;
import org.jetbrains.annotations.NotNull;

public record FirearmPoseMessage(UUID playerId, int ticks) implements CustomPacketPayload {
   public static final Type<FirearmPoseMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "firearm_pose"));
   public static final StreamCodec<RegistryFriendlyByteBuf, FirearmPoseMessage> STREAM_CODEC = StreamCodec.of(
      FirearmPoseMessage::write,
      FirearmPoseMessage::read
   );

   @Override
   @NotNull
   public Type<FirearmPoseMessage> type() {
      return TYPE;
   }

   private static void write(RegistryFriendlyByteBuf buffer, FirearmPoseMessage message) {
      buffer.writeUUID(message.playerId);
      buffer.writeVarInt(message.ticks);
   }

   private static FirearmPoseMessage read(RegistryFriendlyByteBuf buffer) {
      return new FirearmPoseMessage(buffer.readUUID(), buffer.readVarInt());
   }

   public static void handleData(FirearmPoseMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.CLIENTBOUND) {
         context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
               FirearmPoseClient.apply(message.playerId, message.ticks);
            }
         });
      }
   }
}
