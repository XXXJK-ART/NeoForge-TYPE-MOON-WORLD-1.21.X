package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager;
import org.jetbrains.annotations.NotNull;

public record MasterCommandSpellMessage(int action) implements CustomPacketPayload {
   public static final Type<MasterCommandSpellMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "master_command_spell")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, MasterCommandSpellMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeInt(message.action),
      buffer -> new MasterCommandSpellMessage(buffer.readInt())
   );

   @Override
   @NotNull
   public Type<MasterCommandSpellMessage> type() {
      return TYPE;
   }

   public static void handleData(MasterCommandSpellMessage message, IPayloadContext context) {
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player) {
            MasterStateManager.useCommandSpell(player, message.action);
         }
      });
   }
}
