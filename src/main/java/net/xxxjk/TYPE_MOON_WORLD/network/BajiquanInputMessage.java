package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.martial.BajiquanCombatService;

public record BajiquanInputMessage(int input, boolean down, boolean up) implements CustomPacketPayload {
   public static final Type<BajiquanInputMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "bajiquan_input"));
   public static final StreamCodec<RegistryFriendlyByteBuf, BajiquanInputMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeVarInt(message.input);
         buffer.writeBoolean(message.down);
         buffer.writeBoolean(message.up);
      },
      buffer -> new BajiquanInputMessage(buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean())
   );

   @Override public Type<BajiquanInputMessage> type() { return TYPE; }

   public static void handleData(BajiquanInputMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) return;
      context.enqueueWork(() -> {
         if (message.input >= BajiquanCombatService.INPUT_A && message.input <= BajiquanCombatService.INPUT_DOWN) {
            BajiquanCombatService.handleInput(player, message.input, message.down, message.up);
         }
      });
   }
}
