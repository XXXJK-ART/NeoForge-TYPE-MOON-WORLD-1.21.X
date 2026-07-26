package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoCombatService;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoSchool;

public record KendoInputMessage(int school, int input, boolean down, boolean up) implements CustomPacketPayload {
   public static final Type<KendoInputMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "kendo_input"));
   public static final StreamCodec<RegistryFriendlyByteBuf, KendoInputMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> { buffer.writeVarInt(message.school); buffer.writeVarInt(message.input); buffer.writeBoolean(message.down); buffer.writeBoolean(message.up); },
      buffer -> new KendoInputMessage(buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean())
   );
   @Override public Type<KendoInputMessage> type() { return TYPE; }
   public static void handleData(KendoInputMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) return;
      context.enqueueWork(() -> {
         if (message.school >= 0 && message.school < KendoSchool.values().length
            && message.input >= KendoCombatService.INPUT_A && message.input <= KendoCombatService.INPUT_DOWN)
            KendoCombatService.handleInput(player, KendoSchool.values()[message.school], message.input, message.down, message.up);
      });
   }
}
