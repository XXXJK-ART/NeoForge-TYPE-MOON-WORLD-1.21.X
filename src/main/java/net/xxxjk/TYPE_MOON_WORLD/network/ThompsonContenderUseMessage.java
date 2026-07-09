package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ThompsonContenderItem;
import org.jetbrains.annotations.NotNull;

public record ThompsonContenderUseMessage() implements CustomPacketPayload {
   public static final Type<ThompsonContenderUseMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "thompson_contender_use")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ThompsonContenderUseMessage> STREAM_CODEC = StreamCodec.unit(new ThompsonContenderUseMessage());

   @Override
   @NotNull
   public Type<ThompsonContenderUseMessage> type() {
      return TYPE;
   }

   public static void handleData(ThompsonContenderUseMessage message, IPayloadContext context) {
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            ThompsonContenderItem.handleServerRightClick(player, InteractionHand.MAIN_HAND);
         }
      });
   }
}
