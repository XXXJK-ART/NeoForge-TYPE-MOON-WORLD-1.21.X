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

public record ParacelsusElementSelectionMessage(int element) implements CustomPacketPayload {
   public static final Type<ParacelsusElementSelectionMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "paracelsus_element_selection")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ParacelsusElementSelectionMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeInt(message.element),
      buffer -> new ParacelsusElementSelectionMessage(buffer.readInt())
   );

   @NotNull
   @Override
   public Type<ParacelsusElementSelectionMessage> type() {
      return TYPE;
   }

   public static void handleData(ParacelsusElementSelectionMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && message.element >= 0 && message.element <= 3) {
               ServantCardParacelsusSkills.deploySelectedElementalGuardian(player, message.element);
            }
         }).exceptionally(e -> {
            TYPE_MOON_WORLD.LOGGER.error("Failed to handle ParacelsusElementSelectionMessage", e);
            return null;
         });
      }
   }
}
