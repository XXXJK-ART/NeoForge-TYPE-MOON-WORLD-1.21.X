package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardPaleRiderSkills;

public record PaleRiderSpawnModeMessage(int mode) implements CustomPacketPayload {
   public static final Type<PaleRiderSpawnModeMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "pale_rider_spawn_mode"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PaleRiderSpawnModeMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> buf.writeVarInt(msg.mode), buf -> new PaleRiderSpawnModeMessage(buf.readVarInt()));
   @Override public Type<PaleRiderSpawnModeMessage> type() { return TYPE; }
   public static void handleData(PaleRiderSpawnModeMessage msg, IPayloadContext ctx) {
      ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer player) ServantCardPaleRiderSkills.spawn(player, Math.max(0, Math.min(3, msg.mode))); });
   }
}
