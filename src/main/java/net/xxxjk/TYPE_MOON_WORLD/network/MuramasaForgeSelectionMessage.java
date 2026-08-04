package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSenkoMuramasaSkills;
import org.jetbrains.annotations.NotNull;

public record MuramasaForgeSelectionMessage(int choice) implements CustomPacketPayload {
   public static final Type<MuramasaForgeSelectionMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "muramasa_forge_selection"));
   public static final StreamCodec<RegistryFriendlyByteBuf, MuramasaForgeSelectionMessage> STREAM_CODEC =
      StreamCodec.of((buffer, message) -> buffer.writeInt(message.choice), buffer -> new MuramasaForgeSelectionMessage(buffer.readInt()));

   @Override
   @NotNull
   public Type<MuramasaForgeSelectionMessage> type() {
      return TYPE;
   }

   public static void handleData(MuramasaForgeSelectionMessage message, IPayloadContext context) {
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player && message.choice >= 0 && message.choice <= 3) {
            ServantCardSenkoMuramasaSkills.forge(player, message.choice);
         }
      });
   }
}
