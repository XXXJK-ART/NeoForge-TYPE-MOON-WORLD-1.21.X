package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ClientPacketHandler;
import org.jetbrains.annotations.NotNull;

/** Small client-side white flash used by the Enkidu/EA terminal impact. */
public record DuelScreenFlashMessage(int ticks, float strength) implements CustomPacketPayload {
   public static final Type<DuelScreenFlashMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "duel_screen_flash")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, DuelScreenFlashMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeVarInt(Math.max(1, Math.min(20, message.ticks)));
         buffer.writeFloat(Math.max(0.0F, Math.min(1.0F, message.strength)));
      },
      buffer -> new DuelScreenFlashMessage(Math.max(1, Math.min(20, buffer.readVarInt())),
         Math.max(0.0F, Math.min(1.0F, buffer.readFloat())))
   );

   @Override
   @NotNull
   public Type<DuelScreenFlashMessage> type() { return TYPE; }

   public static void handleData(DuelScreenFlashMessage message, IPayloadContext context) {
      context.enqueueWork(() -> ClientPacketHandler.handleDuelScreenFlash(message.ticks, message.strength));
   }
}
