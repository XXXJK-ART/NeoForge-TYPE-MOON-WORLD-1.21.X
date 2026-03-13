package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record DeleteProjectionStructureMessage(String structureId) implements CustomPacketPayload {
   private static final int MAX_STRUCTURE_ID_LENGTH = 128;
   public static final Type<DeleteProjectionStructureMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "delete_projection_structure")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, DeleteProjectionStructureMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeUtf(message.structureId == null ? "" : message.structureId, 128),
      buffer -> new DeleteProjectionStructureMessage(buffer.readUtf(128))
   );

   @NotNull
   public Type<DeleteProjectionStructureMessage> type() {
      return TYPE;
   }

   public static void handleData(DeleteProjectionStructureMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(
            () -> {
               TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)context.player()
                  .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
               String id = message.structureId == null ? "" : message.structureId;
               if (!id.isEmpty() && isValidStructureId(id)) {
                  if (vars.removeStructureById(id)) {
                     context.player().displayClientMessage(Component.translatable("message.typemoonworld.structure.deleted"), true);
                     CompoundTag delta = new CompoundTag();
                     delta.putString("deleted_structure_id", id);
                     delta.putString("selected_structure_id", vars.projection_selected_structure_id == null ? "" : vars.projection_selected_structure_id);
                     vars.syncProjectionDelta(context.player(), 1, delta);
                  }
               }
            }
         );
      }
   }

   private static boolean isValidStructureId(String id) {
      return id.length() <= 128 && id.matches("[a-zA-Z0-9_\\-]+");
   }
}
