package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantBaseAnimationClient;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantBaseAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;

public record ServantBaseAnimationMessage(UUID entityId, int action) implements CustomPacketPayload {
   public static final Type<ServantBaseAnimationMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_base_animation"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantBaseAnimationMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> { buf.writeUUID(msg.entityId); buf.writeByte(msg.action); },
      buf -> new ServantBaseAnimationMessage(buf.readUUID(), buf.readByte()));

   @Override public Type<ServantBaseAnimationMessage> type() { return TYPE; }

   public static void handleData(ServantBaseAnimationMessage msg, IPayloadContext context) {
      if (context.flow() == PacketFlow.CLIENTBOUND) {
         context.enqueueWork(() -> ServantBaseAnimationClient.play(msg.entityId, msg.action));
      } else {
         context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.getUUID().equals(msg.entityId)) return;
            // Client input controls presentation only. Damage is still handled by the normal attack packet.
            if (msg.action == ServantBaseAction.STOP) {
               ServantCombatDefenseService.broadcastBaseAttack(player, msg.action);
               return;
            }
            if (!ServantBaseAction.isAttack(msg.action) || !player.isAlive()
               || !player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_transformed
               || net.xxxjk.TYPE_MOON_WORLD.chain.service.EnumaChainService.isCasting(player)
               || net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem.cannotAct(player)
               || net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterCarryService.attacksBlocked(player)
               || !ServerPacketRateLimiter.allow(player, "servant_base_attack_animation", 1)) return;
            boolean fist = player.getMainHandItem().isEmpty();
            int expected = ServantBaseAction.isContinuous(msg.action)
               ? (fist ? ServantBaseAction.FIST_COMBO : ServantBaseAction.SWORD_COMBO)
               : (fist ? ServantBaseAction.FIST : ServantBaseAction.SWORD);
            if (msg.action == expected) ServantCombatDefenseService.broadcastBaseAttack(player, msg.action);
         });
      }
   }
}
