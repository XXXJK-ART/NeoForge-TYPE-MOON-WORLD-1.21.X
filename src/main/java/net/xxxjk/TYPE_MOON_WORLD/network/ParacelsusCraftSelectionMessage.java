package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardParacelsusSkills;
import org.jetbrains.annotations.NotNull;

public record ParacelsusCraftSelectionMessage(int choice) implements CustomPacketPayload {
   public static final Type<ParacelsusCraftSelectionMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "paracelsus_craft_selection")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ParacelsusCraftSelectionMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeInt(message.choice),
      buffer -> new ParacelsusCraftSelectionMessage(buffer.readInt())
   );

   @NotNull
   @Override
   public Type<ParacelsusCraftSelectionMessage> type() {
      return TYPE;
   }

   public static void handleData(ParacelsusCraftSelectionMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && message.choice >= 0 && message.choice <= 1) {
               ServantCardParacelsusSkills.craftSelectedParacelsusItem(player, message.choice);
            }
         }).exceptionally(e -> {
            TYPE_MOON_WORLD.LOGGER.error("Failed to handle ParacelsusCraftSelectionMessage", e);
            return null;
         });
      }
   }
}
