package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ArtoriaExcaliburAnimationClient;
import org.jetbrains.annotations.NotNull;

public record ArtoriaExcaliburAnimationMessage(UUID entityId, int actionId, long startTick) implements CustomPacketPayload {
   public static final Type<ArtoriaExcaliburAnimationMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "artoria_excalibur_animation"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ArtoriaExcaliburAnimationMessage> STREAM_CODEC = StreamCodec.of(
      ArtoriaExcaliburAnimationMessage::write, ArtoriaExcaliburAnimationMessage::read);

   @Override @NotNull public Type<ArtoriaExcaliburAnimationMessage> type() { return TYPE; }

   private static void write(RegistryFriendlyByteBuf buffer, ArtoriaExcaliburAnimationMessage message) {
      buffer.writeUUID(message.entityId);
      buffer.writeVarInt(message.actionId);
      buffer.writeLong(message.startTick);
   }

   private static ArtoriaExcaliburAnimationMessage read(RegistryFriendlyByteBuf buffer) {
      return new ArtoriaExcaliburAnimationMessage(buffer.readUUID(), buffer.readVarInt(), buffer.readLong());
   }

   public static void handleData(ArtoriaExcaliburAnimationMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND) return;
      context.enqueueWork(() -> {
         if (FMLEnvironment.dist == Dist.CLIENT)
            ArtoriaExcaliburAnimationClient.play(message.entityId, message.actionId, message.startTick);
      });
   }
}
