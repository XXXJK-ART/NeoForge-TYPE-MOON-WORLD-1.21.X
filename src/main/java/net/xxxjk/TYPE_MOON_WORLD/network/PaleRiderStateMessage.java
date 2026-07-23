package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.PaleRiderClientState;

public record PaleRiderStateMessage(int controlledCount, boolean possessing, boolean underworld, boolean calamity, boolean perfectStealth) implements CustomPacketPayload {
   public static final Type<PaleRiderStateMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "pale_rider_state"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PaleRiderStateMessage> STREAM_CODEC = StreamCodec.of((buf, msg) -> {
      buf.writeVarInt(msg.controlledCount); buf.writeBoolean(msg.possessing); buf.writeBoolean(msg.underworld); buf.writeBoolean(msg.calamity); buf.writeBoolean(msg.perfectStealth);
   }, buf -> new PaleRiderStateMessage(buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));
   @Override public Type<PaleRiderStateMessage> type() { return TYPE; }
   public static void handleData(PaleRiderStateMessage msg, IPayloadContext ctx) {
      ctx.enqueueWork(() -> PaleRiderClientState.update(msg.controlledCount, msg.possessing, msg.underworld, msg.calamity, msg.perfectStealth));
   }
}
