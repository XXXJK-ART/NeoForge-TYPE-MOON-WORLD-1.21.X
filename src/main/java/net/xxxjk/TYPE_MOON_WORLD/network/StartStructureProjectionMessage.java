package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.StructureProjectionBuildHandler;
import org.jetbrains.annotations.NotNull;

public record StartStructureProjectionMessage(String structureId, int anchorX, int anchorY, int anchorZ, int rotationIndex) implements CustomPacketPayload {
   private static final int MAX_STRUCTURE_ID_LENGTH = 128;
   private static final int MAX_ANCHOR_DISTANCE_SQR = 9216;
   public static final Type<StartStructureProjectionMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "start_structure_projection")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, StartStructureProjectionMessage> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
      buffer.writeUtf(message.structureId == null ? "" : message.structureId, 128);
      buffer.writeInt(message.anchorX);
      buffer.writeInt(message.anchorY);
      buffer.writeInt(message.anchorZ);
      buffer.writeInt(message.rotationIndex);
   }, buffer -> new StartStructureProjectionMessage(buffer.readUtf(128), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()));

   @NotNull
   public Type<StartStructureProjectionMessage> type() {
      return TYPE;
   }

   public static void handleData(StartStructureProjectionMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
               String structureId = message.structureId == null ? "" : message.structureId.trim();
               if (structureId.isEmpty() || isValidStructureId(structureId)) {
                  if (message.rotationIndex >= 0 && message.rotationIndex <= 3) {
                     BlockPos anchorPos = new BlockPos(message.anchorX, message.anchorY, message.anchorZ);
                     if (player.level().isLoaded(anchorPos)) {
                        if (!(player.blockPosition().distSqr(anchorPos) > 9216.0)) {
                           if (!StructureProjectionBuildHandler.startProjection(player, structureId, anchorPos, message.rotationIndex)) {
                              player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.start_failed"), true);
                           }
                        }
                     }
                  }
               }
            }
         }).exceptionally(e -> {
            TYPE_MOON_WORLD.LOGGER.error("Failed to handle StartStructureProjectionMessage", e);
            if (context.player() != null) {
               context.player().displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.start_failed"), true);
            }

            return null;
         });
      }
   }

   private static boolean isValidStructureId(String id) {
      return id.length() <= 128 && id.matches("[a-zA-Z0-9_\\-]+");
   }
}
