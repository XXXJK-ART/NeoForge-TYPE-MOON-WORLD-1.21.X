package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import org.jetbrains.annotations.NotNull;

public record SelectProjectionStructureMessage(String structureId) implements CustomPacketPayload {
   private static final int MAX_STRUCTURE_ID_LENGTH = 128;
   public static final Type<SelectProjectionStructureMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "select_projection_structure")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, SelectProjectionStructureMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeUtf(message.structureId == null ? "" : message.structureId, 128),
      buffer -> new SelectProjectionStructureMessage(buffer.readUtf(128))
   );

   @NotNull
   public Type<SelectProjectionStructureMessage> type() {
      return TYPE;
   }

   public static void handleData(SelectProjectionStructureMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(
               () -> {
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)context.player()
                     .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                  String id = message.structureId == null ? "" : message.structureId;
                  if (id.isEmpty() || isValidStructureId(id)) {
                     if (!id.isEmpty() && vars.getStructureById(id) != null) {
                        if (id.equals(vars.projection_selected_structure_id)) {
                           vars.projection_selected_structure_id = "";
                        } else {
                           vars.projection_selected_structure_id = id;
                           vars.projection_selected_item = ItemStack.EMPTY;
                        }
                     } else {
                        vars.projection_selected_structure_id = "";
                     }

                     CompoundTag delta = new CompoundTag();
                     delta.putString("selected_structure_id", vars.projection_selected_structure_id == null ? "" : vars.projection_selected_structure_id);
                     if (!vars.projection_selected_structure_id.isEmpty()) {
                        delta.putBoolean("clear_selected_item", true);
                     }
                     vars.syncProjectionDelta(context.player(), 2, delta);
                     PlayerMagicSelectionService.syncPresetMutation(context.player(), vars);
                  }
               }
            )
            .exceptionally(e -> {
               TYPE_MOON_WORLD.LOGGER.error("Failed to handle SelectProjectionStructureMessage", e);
               return null;
            });
      }
   }

   private static boolean isValidStructureId(String id) {
      return id.length() <= 128 && id.matches("[a-zA-Z0-9_\\-]+");
   }
}
