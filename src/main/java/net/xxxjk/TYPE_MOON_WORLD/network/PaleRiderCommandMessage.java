package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardPaleRiderSkills;

public record PaleRiderCommandMessage(int command) implements CustomPacketPayload {
   public static final Type<PaleRiderCommandMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "pale_rider_command"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PaleRiderCommandMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> buf.writeVarInt(msg.command), buf -> new PaleRiderCommandMessage(buf.readVarInt()));
   @Override public Type<PaleRiderCommandMessage> type() { return TYPE; }
   public static void handleData(PaleRiderCommandMessage msg, IPayloadContext ctx) {
      ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer player) ServantCardPaleRiderSkills.setCommand(player, msg.command); });
   }
}
