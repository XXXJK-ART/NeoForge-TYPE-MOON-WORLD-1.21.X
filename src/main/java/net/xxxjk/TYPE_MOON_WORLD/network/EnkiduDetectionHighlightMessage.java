package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ClientPacketHandler;
import org.jetbrains.annotations.NotNull;

public record EnkiduDetectionHighlightMessage(List<Integer> entityIds, int ticks) implements CustomPacketPayload {
   private static final int MAX_ENTITY_IDS = 256;
   private static final int MAX_TICKS = 20 * 30;
   public static final Type<EnkiduDetectionHighlightMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "enkidu_detection_highlight")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, EnkiduDetectionHighlightMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         int size = Math.min(MAX_ENTITY_IDS, message.entityIds == null ? 0 : message.entityIds.size());
         buffer.writeVarInt(size);
         for (int i = 0; i < size; i++) {
            Integer id = message.entityIds.get(i);
            buffer.writeVarInt(id == null ? 0 : id);
         }
         buffer.writeVarInt(Math.max(1, Math.min(MAX_TICKS, message.ticks)));
      },
      buffer -> {
         int size = Math.max(0, Math.min(MAX_ENTITY_IDS, buffer.readVarInt()));
         List<Integer> ids = new ArrayList<>(size);
         for (int i = 0; i < size; i++) {
            ids.add(buffer.readVarInt());
         }
         return new EnkiduDetectionHighlightMessage(ids, Math.max(1, Math.min(MAX_TICKS, buffer.readVarInt())));
      }
   );

   @Override
   @NotNull
   public Type<EnkiduDetectionHighlightMessage> type() {
      return TYPE;
   }

   public static void handleData(EnkiduDetectionHighlightMessage message, IPayloadContext context) {
      context.enqueueWork(() -> ClientPacketHandler.handleEnkiduDetectionHighlight(message.entityIds, message.ticks));
   }
}
