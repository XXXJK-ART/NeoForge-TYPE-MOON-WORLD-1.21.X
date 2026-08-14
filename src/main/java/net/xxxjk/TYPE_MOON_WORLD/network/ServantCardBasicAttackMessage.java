package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardOdaNobunagaSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardGilgameshSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardCasterGilgameshSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHeraclesSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardDiarmuidSkills;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.jetbrains.annotations.NotNull;

public record ServantCardBasicAttackMessage(boolean secondary) implements CustomPacketPayload {
   public static final Type<ServantCardBasicAttackMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_card_basic_attack")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantCardBasicAttackMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeBoolean(message.secondary),
      buffer -> new ServantCardBasicAttackMessage(buffer.readBoolean())
   );

   @Override
   @NotNull
   public Type<ServantCardBasicAttackMessage> type() {
      return TYPE;
   }

   public static void handleData(ServantCardBasicAttackMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND) return;
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            boolean diarmuidDualWield = vars.servant_card_transformed
               && "diarmuid_ua_duibhne".equals(vars.servant_card_id)
               && ServantCardDiarmuidSkills.isDualWieldActive(player);
            String rateLimitKey = diarmuidDualWield
               ? (message.secondary ? "servant_card_diarmuid_mainhand_attack" : "servant_card_diarmuid_offhand_attack")
               : "servant_card_basic_attack";
            if (!ServerPacketRateLimiter.allow(player, rateLimitKey, 1)) {
               return;
            }
            if (vars.servant_card_transformed && "shadow_hassan".equals(vars.servant_card_id)) {
               if (!net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardShadowHassanSkills.canAttack(player)) return;
               net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardShadowHassanSkills.revealForAttack(player);
            }
            if (player.getMainHandItem().is(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.TEMPLE_STONE_SWORD_AXE.get())) {
               ServantCardHeraclesSkills.performBasicSweep(player);
            } else if (vars.servant_card_transformed && "gilgamesh".equals(vars.servant_card_id) && message.secondary) {
               ServantCardGilgameshSkills.performSingleVault(player);
            } else if (vars.servant_card_transformed && "gilgamesh_caster".equals(vars.servant_card_id) && message.secondary) {
               ServantCardCasterGilgameshSkills.performSlateBasic(player);
            } else if (diarmuidDualWield) {
               if (message.secondary) {
                  ServantCardDiarmuidSkills.performMainhandSpearThrust(player);
               } else {
                  ServantCardDiarmuidSkills.performOffhandSpearThrust(player);
               }
            } else {
               ServantCardOdaNobunagaSkills.handleBasicAttackPacket(player, message.secondary);
            }
         }
      });
   }
}
