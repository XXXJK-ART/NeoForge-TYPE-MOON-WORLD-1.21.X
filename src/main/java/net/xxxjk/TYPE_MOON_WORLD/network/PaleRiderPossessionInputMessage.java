package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardPaleRiderSkills;

public record PaleRiderPossessionInputMessage(float forward, float strafe, float vertical, float yaw, float pitch, boolean attack) implements CustomPacketPayload {
   public static final Type<PaleRiderPossessionInputMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "pale_rider_possession_input"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PaleRiderPossessionInputMessage> STREAM_CODEC = StreamCodec.of((buf, msg) -> {
      buf.writeFloat(msg.forward); buf.writeFloat(msg.strafe); buf.writeFloat(msg.vertical); buf.writeFloat(msg.yaw); buf.writeFloat(msg.pitch); buf.writeBoolean(msg.attack);
   }, buf -> new PaleRiderPossessionInputMessage(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean()));
   @Override public Type<PaleRiderPossessionInputMessage> type() { return TYPE; }
   public static void handleData(PaleRiderPossessionInputMessage msg, IPayloadContext ctx) {
      ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer player) ServantCardPaleRiderSkills.applyPossessionInput(player, msg); });
   }
}
