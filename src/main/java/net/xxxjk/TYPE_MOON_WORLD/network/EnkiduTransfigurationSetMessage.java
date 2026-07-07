package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardEnkiduSkills;
import org.jetbrains.annotations.NotNull;

public record EnkiduTransfigurationSetMessage(int strength, int endurance, int agility, int mana, int luck) implements CustomPacketPayload {
   public static final Type<EnkiduTransfigurationSetMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "enkidu_transfiguration_set")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, EnkiduTransfigurationSetMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeVarInt(message.strength);
         buffer.writeVarInt(message.endurance);
         buffer.writeVarInt(message.agility);
         buffer.writeVarInt(message.mana);
         buffer.writeVarInt(message.luck);
      },
      buffer -> new EnkiduTransfigurationSetMessage(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt())
   );

   @NotNull
   @Override
   public Type<EnkiduTransfigurationSetMessage> type() {
      return TYPE;
   }

   public static void handleData(EnkiduTransfigurationSetMessage message, IPayloadContext context) {
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            ServantCardEnkiduSkills.setTransfigurationPoints(
               player,
               new int[]{message.strength, message.endurance, message.agility, message.mana, message.luck}
            );
         }
      });
   }
}
