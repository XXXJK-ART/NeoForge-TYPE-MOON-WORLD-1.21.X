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
import net.xxxjk.TYPE_MOON_WORLD.client.ClientPacketHandler;
import org.jetbrains.annotations.NotNull;

public record MasterVisualStateMessage(UUID playerId, boolean masterActive, int commandSpells, boolean poseActive) implements CustomPacketPayload {
   public static final Type<MasterVisualStateMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "master_visual_state")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, MasterVisualStateMessage> STREAM_CODEC = StreamCodec.of(
      MasterVisualStateMessage::write,
      MasterVisualStateMessage::read
   );

   @Override
   @NotNull
   public Type<MasterVisualStateMessage> type() {
      return TYPE;
   }

   private static void write(RegistryFriendlyByteBuf buffer, MasterVisualStateMessage message) {
      buffer.writeUUID(message.playerId);
      buffer.writeBoolean(message.masterActive);
      buffer.writeVarInt(message.commandSpells);
      buffer.writeBoolean(message.poseActive);
   }

   private static MasterVisualStateMessage read(RegistryFriendlyByteBuf buffer) {
      return new MasterVisualStateMessage(buffer.readUUID(), buffer.readBoolean(), buffer.readVarInt(), buffer.readBoolean());
   }

   public static void handleData(MasterVisualStateMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.CLIENTBOUND) {
         context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
               ClientPacketHandler.handleMasterVisualState(message.playerId, message.masterActive, message.commandSpells, message.poseActive);
            }
         });
      }
   }
}
