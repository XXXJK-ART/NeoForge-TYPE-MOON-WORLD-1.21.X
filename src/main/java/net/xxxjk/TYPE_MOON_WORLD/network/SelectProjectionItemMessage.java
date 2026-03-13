package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import org.jetbrains.annotations.NotNull;

public record SelectProjectionItemMessage(int index) implements CustomPacketPayload {
   public static final Type<SelectProjectionItemMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "select_projection_item"));
   public static final StreamCodec<FriendlyByteBuf, SelectProjectionItemMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeInt(message.index), buffer -> new SelectProjectionItemMessage(buffer.readInt())
   );

   @NotNull
   public Type<SelectProjectionItemMessage> type() {
      return TYPE;
   }

   public static void handleData(SelectProjectionItemMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(
               () -> {
                  if (context.player() instanceof ServerPlayer player) {
                     TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                        TypeMoonWorldModVariables.PLAYER_VARIABLES
                     );
                     if (message.index >= 0 && message.index < vars.analyzed_items.size()) {
                        vars.projection_selected_item = vars.analyzed_items.get(message.index).copy();
                        vars.projection_selected_structure_id = "";
                        CompoundTag delta = new CompoundTag();
                        delta.putBoolean("clear_selected_structure", true);
                        delta.put("selected_item", vars.projection_selected_item.save(player.registryAccess()));
                        vars.syncProjectionDelta(player, 2, delta);
                     }
                  }
               }
            )
            .exceptionally(e -> {
               TYPE_MOON_WORLD.LOGGER.error("Failed to handle SelectProjectionItemMessage", e);
               return null;
            });
      }
   }
}
