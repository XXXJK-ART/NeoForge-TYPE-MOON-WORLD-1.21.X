package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** One visual contact notification per successful Mumyoudan hit; carries no gameplay changes. */
public record OkitaMumyoudanImpactMessage(int attackerId, int targetId, int strike) implements CustomPacketPayload {
   public static final Type<OkitaMumyoudanImpactMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "okita_mumyoudan_impact"));
   public static final StreamCodec<RegistryFriendlyByteBuf, OkitaMumyoudanImpactMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> { buffer.writeVarInt(message.attackerId); buffer.writeVarInt(message.targetId); buffer.writeByte(message.strike); },
      buffer -> new OkitaMumyoudanImpactMessage(buffer.readVarInt(), buffer.readVarInt(), buffer.readUnsignedByte()));

   @Override @NotNull public Type<OkitaMumyoudanImpactMessage> type() { return TYPE; }

   public static void handleData(OkitaMumyoudanImpactMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND || message.strike < 0 || message.strike > 2) return;
      context.enqueueWork(() -> {
         if (FMLEnvironment.dist == Dist.CLIENT)
            net.xxxjk.TYPE_MOON_WORLD.client.OkitaMumyoudanImpactClient.receive(message);
      });
   }
}
