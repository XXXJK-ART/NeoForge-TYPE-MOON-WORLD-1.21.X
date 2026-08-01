package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterVisualStateSync;
import org.jetbrains.annotations.NotNull;

public record MasterCommandSpellPoseMessage(boolean active) implements CustomPacketPayload {
   public static final Type<MasterCommandSpellPoseMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "master_command_spell_pose")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, MasterCommandSpellPoseMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeBoolean(message.active),
      buffer -> new MasterCommandSpellPoseMessage(buffer.readBoolean())
   );

   @Override
   @NotNull
   public Type<MasterCommandSpellPoseMessage> type() {
      return TYPE;
   }

   public static void handleData(MasterCommandSpellPoseMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND) return;
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player
            && ServerPacketRateLimiter.allow(player, "master_command_spell_pose", 1)) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            boolean active = message.active && vars.master_active;
            if (vars.master_command_spell_pose_active == active) return;
            vars.master_command_spell_pose_active = active;
            MasterVisualStateSync.broadcast(player, vars);
         }
      });
   }
}
