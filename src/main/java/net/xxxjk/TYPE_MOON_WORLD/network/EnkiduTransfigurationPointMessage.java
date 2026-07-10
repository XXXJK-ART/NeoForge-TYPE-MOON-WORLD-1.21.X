package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardEnkiduSkills;
import org.jetbrains.annotations.NotNull;

public record EnkiduTransfigurationPointMessage(int stat, int delta) implements CustomPacketPayload {
   public static final Type<EnkiduTransfigurationPointMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "enkidu_transfiguration_point")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, EnkiduTransfigurationPointMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeInt(message.stat);
         buffer.writeInt(message.delta);
      },
      buffer -> new EnkiduTransfigurationPointMessage(buffer.readInt(), buffer.readInt())
   );

   @Override
   @NotNull
   public Type<EnkiduTransfigurationPointMessage> type() {
      return TYPE;
   }

   public static void handleData(EnkiduTransfigurationPointMessage message, IPayloadContext context) {
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            ServantCardEnkiduSkills.adjustTransfigurationPoint(player, message.stat, message.delta);
         }
      });
   }
}
