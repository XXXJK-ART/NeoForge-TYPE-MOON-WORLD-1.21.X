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

public record CycleMagicMessage(boolean forward) implements CustomPacketPayload {
   public static final Type<CycleMagicMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "cycle_magic"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CycleMagicMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeBoolean(message.forward), buffer -> new CycleMagicMessage(buffer.readBoolean())
   );

   public Type<CycleMagicMessage> type() {
      return TYPE;
   }

   public static void handleData(CycleMagicMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(
               () -> {
                  Player player = context.player();
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                     TypeMoonWorldModVariables.PLAYER_VARIABLES
                  );
                  vars.ensureMagicSystemInitialized();
                  vars.rebuildSelectedMagicsFromActiveWheel();
                  if (vars.selected_magics.isEmpty()) {
                     vars.current_magic_index = 0;
                  } else if (message.forward) {
                     vars.current_magic_index++;
                     if (vars.current_magic_index >= vars.selected_magics.size()) {
                        vars.current_magic_index = 0;
                     }
                  } else {
                     vars.current_magic_index--;
                     if (vars.current_magic_index < 0) {
                        vars.current_magic_index = vars.selected_magics.size() - 1;
                     }
                  }

                  PlayerMagicSelectionService.syncCurrentSelection(player, vars);
               }
            )
            .exceptionally(e -> {
               TYPE_MOON_WORLD.LOGGER.error("Failed to handle CycleMagicMessage", e);
               return null;
            });
      }
   }
}
