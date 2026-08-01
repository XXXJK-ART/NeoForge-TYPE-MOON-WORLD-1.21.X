package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager;
import org.jetbrains.annotations.NotNull;

public record ServantMasterContractMessage(int targetEntityId) implements CustomPacketPayload {
   public static final Type<ServantMasterContractMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_master_contract")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantMasterContractMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeInt(message.targetEntityId),
      buffer -> new ServantMasterContractMessage(buffer.readInt())
   );

   @Override
   @NotNull
   public Type<ServantMasterContractMessage> type() {
      return TYPE;
   }

   public static void handleData(ServantMasterContractMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND || message.targetEntityId < 0) return;
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer actor
            && ServerPacketRateLimiter.allow(actor, "servant_master_contract", 10)) {
            Entity target = actor.level().getEntity(message.targetEntityId);
            ItemStack contract = actor.getMainHandItem().is(ModItems.SERVANT_MASTER_CONTRACT.get())
               ? actor.getMainHandItem() : actor.getOffhandItem();
            if (target instanceof ServerPlayer other
               && actor.isAlive() && other.isAlive()
               && actor.level() == other.level()
               && actor.distanceToSqr(other) <= 36.0
               && contract.is(ModItems.SERVANT_MASTER_CONTRACT.get())
               && MasterStateManager.bindByContract(actor, other)
               && !actor.getAbilities().instabuild) {
               contract.shrink(1);
            }
         }
      });
   }
}
