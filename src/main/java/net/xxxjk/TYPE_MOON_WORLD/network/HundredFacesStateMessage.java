package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.HundredFacesClientState;

public record HundredFacesStateMessage(int count, boolean attackEnabled) implements CustomPacketPayload {
   public static final Type<HundredFacesStateMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "hundred_faces_state"));
   public static final StreamCodec<RegistryFriendlyByteBuf, HundredFacesStateMessage> STREAM_CODEC = StreamCodec.of((buf, msg) -> {
      buf.writeVarInt(msg.count);
      buf.writeBoolean(msg.attackEnabled);
   }, buf -> new HundredFacesStateMessage(buf.readVarInt(), buf.readBoolean()));

   @Override
   public Type<HundredFacesStateMessage> type() {
      return TYPE;
   }

   public static void handleData(HundredFacesStateMessage msg, IPayloadContext ctx) {
      ctx.enqueueWork(() -> HundredFacesClientState.update(msg.count, msg.attackEnabled));
   }
}
