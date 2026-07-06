package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
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
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer actor) {
            Entity target = actor.level().getEntity(message.targetEntityId);
            if (target instanceof ServerPlayer other) {
               MasterStateManager.bindByContract(actor, other);
            }
         }
      });
   }
}
