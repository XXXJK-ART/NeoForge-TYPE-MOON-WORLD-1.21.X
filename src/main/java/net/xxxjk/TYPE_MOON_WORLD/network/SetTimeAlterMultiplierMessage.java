package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetTimeAlterMultiplierMessage(int multiplier) implements CustomPacketPayload {
   public static final Type<SetTimeAlterMultiplierMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "set_time_alter_multiplier")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, SetTimeAlterMultiplierMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeVarInt(message.multiplier),
      buffer -> new SetTimeAlterMultiplierMessage(buffer.readVarInt())
   );

   @Override
   @NotNull
   public Type<SetTimeAlterMultiplierMessage> type() {
      return TYPE;
   }

   public static void handleData(SetTimeAlterMultiplierMessage message, IPayloadContext context) {
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (vars.proficiency_time_alter >= 80.0 && message.multiplier >= 1) {
               vars.time_alter_multiplier = message.multiplier;
               vars.syncPlayerVariables(player);
               net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService.syncPresetMutation(player, vars);
            }
         }
      });
   }
}
