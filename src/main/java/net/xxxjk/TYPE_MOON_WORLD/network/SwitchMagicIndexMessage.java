package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;

public record SwitchMagicIndexMessage(int runtimeIndex) implements CustomPacketPayload {
   public static final Type<SwitchMagicIndexMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "switch_magic_index"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SwitchMagicIndexMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeInt(message.runtimeIndex), buffer -> new SwitchMagicIndexMessage(buffer.readInt())
   );

   public Type<SwitchMagicIndexMessage> type() {
      return TYPE;
   }

   public static void handleData(SwitchMagicIndexMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(
               () -> {
                  Player player = context.player();
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                     TypeMoonWorldModVariables.PLAYER_VARIABLES
                  );
                  vars.ensureMagicSystemInitialized();
                  vars.rebuildSelectedMagicsFromActiveWheel();
                  int idx = message.runtimeIndex;
                  if (idx >= 0 && idx < vars.selected_magics.size()) {
                     vars.current_magic_index = idx;
                  }

                  PlayerMagicSelectionService.syncCurrentSelection(player, vars);
               }
            )
            .exceptionally(e -> {
               TYPE_MOON_WORLD.LOGGER.error("Failed to handle SwitchMagicIndexMessage", e);
               return null;
            });
      }
   }
}
