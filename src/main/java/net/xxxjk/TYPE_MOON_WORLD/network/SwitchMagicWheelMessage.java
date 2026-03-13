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

public record SwitchMagicWheelMessage(int wheelIndex) implements CustomPacketPayload {
   public static final Type<SwitchMagicWheelMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "switch_magic_wheel"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SwitchMagicWheelMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeInt(message.wheelIndex), buffer -> new SwitchMagicWheelMessage(buffer.readInt())
   );

   public Type<SwitchMagicWheelMessage> type() {
      return TYPE;
   }

   public static void handleData(SwitchMagicWheelMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(
               () -> {
                  Player player = context.player();
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                     TypeMoonWorldModVariables.PLAYER_VARIABLES
                  );
                  vars.ensureMagicSystemInitialized();
                  int wheel = message.wheelIndex;
                  if (wheel >= 0 && wheel < 10) {
                     vars.switchActiveWheel(wheel);
                     vars.syncRuntimeSelection(player);
                  } else {
                     vars.syncRuntimeSelection(player);
                  }
               }
            )
            .exceptionally(e -> {
               TYPE_MOON_WORLD.LOGGER.error("Failed to handle SwitchMagicWheelMessage", e);
               return null;
            });
      }
   }
}
