package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.martial.GanryuCombatService;

public record GanryuInputMessage(int input, boolean down, boolean up) implements CustomPacketPayload {
   public static final Type<GanryuInputMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "ganryu_input"));
   public static final StreamCodec<RegistryFriendlyByteBuf, GanryuInputMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeVarInt(message.input);
         buffer.writeBoolean(message.down);
         buffer.writeBoolean(message.up);
      },
      buffer -> new GanryuInputMessage(buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean())
   );

   @Override public Type<GanryuInputMessage> type() { return TYPE; }

   public static void handleData(GanryuInputMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) return;
      context.enqueueWork(() -> {
         if (message.input >= GanryuCombatService.INPUT_A && message.input <= GanryuCombatService.INPUT_DOWN) {
            GanryuCombatService.handleInput(player, message.input, message.down, message.up);
         }
      });
   }
}
