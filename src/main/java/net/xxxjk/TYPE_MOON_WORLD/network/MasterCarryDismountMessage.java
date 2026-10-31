package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterCarryService;

public record MasterCarryDismountMessage() implements CustomPacketPayload {
   public static final Type<MasterCarryDismountMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "master_carry_dismount"));
   public static final StreamCodec<RegistryFriendlyByteBuf, MasterCarryDismountMessage> STREAM_CODEC = StreamCodec.unit(new MasterCarryDismountMessage());
   @Override public Type<MasterCarryDismountMessage> type() { return TYPE; }
   public static void handle(MasterCarryDismountMessage message, IPayloadContext context) {
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player && ServerPacketRateLimiter.allow(player, "carry_dismount", 1)
            && (ServantMasterCarryService.isCarryingMaster(player) || ServantMasterCarryService.isCarriedMaster(player))) {
            ServantMasterCarryService.dismount(player);
         }
      });
   }
}
