package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.GemCarvingTableMenu;
import org.jetbrains.annotations.NotNull;

public record GemCarvingEngraveMessage(
   String magicId, int reinforcementPart, int reinforcementLevel, int projectionMode, int projectionItemIndex, String projectionStructureId
) implements CustomPacketPayload {
   private static final int MAX_MAGIC_ID_LENGTH = 64;
   private static final int MAX_STRUCTURE_ID_LENGTH = 128;
   public static final Type<GemCarvingEngraveMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "gem_carving_engrave"));
   public static final StreamCodec<RegistryFriendlyByteBuf, GemCarvingEngraveMessage> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
      buffer.writeUtf(message.magicId == null ? "" : message.magicId, 64);
      buffer.writeInt(message.reinforcementPart);
      buffer.writeInt(message.reinforcementLevel);
      buffer.writeInt(message.projectionMode);
      buffer.writeInt(message.projectionItemIndex);
      buffer.writeUtf(message.projectionStructureId == null ? "" : message.projectionStructureId, 128);
   }, buffer -> new GemCarvingEngraveMessage(buffer.readUtf(64), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readUtf(128)));

   @NotNull
   public Type<GemCarvingEngraveMessage> type() {
      return TYPE;
   }

   public static void handleData(GemCarvingEngraveMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(
               () -> {
                  if (context.player() instanceof ServerPlayer player) {
                     if (isValidMagicId(message.magicId)) {
                        if (player.containerMenu instanceof GemCarvingTableMenu menu) {
                           menu.tryEngrave(
                              player,
                              message.magicId,
                              message.reinforcementPart,
                              message.reinforcementLevel,
                              message.projectionMode,
                              message.projectionItemIndex,
                              message.projectionStructureId
                           );
                        }
                     }
                  }
               }
            )
            .exceptionally(e -> {
               TYPE_MOON_WORLD.LOGGER.error("Failed to handle GemCarvingEngraveMessage", e);
               return null;
            });
      }
   }

   private static boolean isValidMagicId(String magicId) {
      return magicId != null && !magicId.isEmpty() && magicId.length() <= 64 && magicId.matches("[a-z0-9_]+");
   }
}
