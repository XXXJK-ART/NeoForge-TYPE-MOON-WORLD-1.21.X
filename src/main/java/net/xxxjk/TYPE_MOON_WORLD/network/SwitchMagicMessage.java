package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;

public record SwitchMagicMessage(String magicId) implements CustomPacketPayload {
   public static final Type<SwitchMagicMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "switch_magic"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SwitchMagicMessage> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, SwitchMagicMessage::magicId, SwitchMagicMessage::new
   );

   public Type<SwitchMagicMessage> type() {
      return TYPE;
   }

   public static void handleData(SwitchMagicMessage message, IPayloadContext context) {
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
                  } else {
                     int size = vars.selected_magics.size();
                     int start = Mth.clamp(vars.current_magic_index, 0, size - 1);
                     int chosen = -1;

                     for (int i = 1; i <= size; i++) {
                        int idx = (start + i) % size;
                        if (message.magicId.equals(vars.selected_magics.get(idx))) {
                           chosen = idx;
                           break;
                        }
                     }

                     if (chosen >= 0) {
                        vars.current_magic_index = chosen;
                     }
                  }

                  PlayerMagicSelectionService.syncCurrentSelection(player, vars);
               }
            )
            .exceptionally(e -> {
               TYPE_MOON_WORLD.LOGGER.error("Failed to handle SwitchMagicMessage", e);
               return null;
            });
      }
   }
}
