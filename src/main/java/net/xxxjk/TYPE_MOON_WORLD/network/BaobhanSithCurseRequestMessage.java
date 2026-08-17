package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardBaobhanSithSkills;

public record BaobhanSithCurseRequestMessage() implements CustomPacketPayload {
   public static final Type<BaobhanSithCurseRequestMessage> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "baobhan_sith_curse_request"));
   public static final StreamCodec<RegistryFriendlyByteBuf, BaobhanSithCurseRequestMessage> STREAM_CODEC =
      StreamCodec.unit(new BaobhanSithCurseRequestMessage());

   @Override
   public Type<BaobhanSithCurseRequestMessage> type() {
      return TYPE;
   }

   public static void handleData(BaobhanSithCurseRequestMessage msg, IPayloadContext ctx) {
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player) {
            ServantCardBaobhanSithSkills.refreshCursePanel(player);
         }
      });
   }
}
