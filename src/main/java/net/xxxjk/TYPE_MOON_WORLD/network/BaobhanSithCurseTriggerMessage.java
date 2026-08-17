package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardBaobhanSithSkills;

public record BaobhanSithCurseTriggerMessage(UUID targetId, String medium, boolean selectOnly, boolean noblePhantasm, boolean allTargets) implements CustomPacketPayload {
   public static final Type<BaobhanSithCurseTriggerMessage> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "baobhan_sith_curse_trigger"));
   public static final StreamCodec<RegistryFriendlyByteBuf, BaobhanSithCurseTriggerMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> {
         buf.writeUUID(msg.targetId);
         buf.writeUtf(msg.medium == null ? "" : msg.medium, 24);
         buf.writeBoolean(msg.selectOnly);
         buf.writeBoolean(msg.noblePhantasm);
         buf.writeBoolean(msg.allTargets);
      },
      buf -> new BaobhanSithCurseTriggerMessage(buf.readUUID(), buf.readUtf(24), buf.readBoolean(), buf.readBoolean(), buf.readBoolean())
   );

   @Override
   public Type<BaobhanSithCurseTriggerMessage> type() {
      return TYPE;
   }

   public static void handleData(BaobhanSithCurseTriggerMessage msg, IPayloadContext ctx) {
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player) {
            if (msg.noblePhantasm && msg.allTargets) {
               ServantCardBaobhanSithSkills.detonateAllFetchFailnaught(player);
            } else if (msg.noblePhantasm) {
               ServantCardBaobhanSithSkills.detonateFetchFailnaught(player, msg.targetId);
            } else if (msg.selectOnly) {
               ServantCardBaobhanSithSkills.selectPanelTarget(player, msg.targetId);
            } else {
               ServantCardBaobhanSithSkills.triggerPanelMedium(player, msg.targetId, msg.medium);
            }
         }
      });
   }
}
