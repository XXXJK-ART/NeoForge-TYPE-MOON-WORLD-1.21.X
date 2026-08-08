package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import org.jetbrains.annotations.NotNull;

public record DeleteProjectionItemMessage(int index, ItemStack expected) implements CustomPacketPayload {
   public static final Type<DeleteProjectionItemMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "delete_projection_item"));
   public static final StreamCodec<RegistryFriendlyByteBuf, DeleteProjectionItemMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeVarInt(message.index);
         ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, message.expected);
      },
      buffer -> new DeleteProjectionItemMessage(buffer.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer))
   );

   @Override public @NotNull Type<DeleteProjectionItemMessage> type() { return TYPE; }

   public static void handleData(DeleteProjectionItemMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND) return;
      context.enqueueWork(() -> {
         if (!(context.player() instanceof ServerPlayer player)) return;
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (message.index < 0 || message.index >= vars.analyzed_items.size()) return;
         ItemStack stored = vars.analyzed_items.get(message.index);
         if (!ItemStack.isSameItemSameComponents(stored, message.expected)) return;
         boolean selected = ItemStack.isSameItemSameComponents(stored, vars.projection_selected_item);
         vars.analyzed_items.remove(message.index);
         if (selected) vars.projection_selected_item = ItemStack.EMPTY;
         CompoundTag delta = new CompoundTag();
         delta.putInt("deleted_item_index", message.index);
         if (selected) delta.putBoolean("clear_selected_item", true);
         vars.syncProjectionDelta(player, TypeMoonWorldModVariables.ProjectionDeltaSyncMessage.ACTION_DELETE, delta);
         PlayerMagicSelectionService.syncPresetMutation(player, vars);
      }).exceptionally(error -> {
         TYPE_MOON_WORLD.LOGGER.error("Failed to delete projection library item", error);
         return null;
      });
   }
}
