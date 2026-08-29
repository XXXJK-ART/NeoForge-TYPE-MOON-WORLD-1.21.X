package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgram;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgramService;

/** CRUD packet for the server-authoritative rune program library. */
public record RuneProgramMessage(int action, CompoundTag program, String uuid) implements CustomPacketPayload {
   public static final int UPSERT = 0, DELETE = 1, COPY = 2;
   public static final Type<RuneProgramMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "rune_program"));
   public static final StreamCodec<RegistryFriendlyByteBuf, RuneProgramMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> { buf.writeVarInt(msg.action); buf.writeNbt(msg.program == null ? new CompoundTag() : msg.program); buf.writeUtf(msg.uuid == null ? "" : msg.uuid, 64); },
      buf -> new RuneProgramMessage(buf.readVarInt(), buf.readNbt(), buf.readUtf(64)));
   @Override public Type<RuneProgramMessage> type() { return TYPE; }
   public static void handleData(RuneProgramMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND) return;
      context.enqueueWork(() -> {
         if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) return;
         try {
            UUID id = message.uuid == null || message.uuid.isBlank() ? null : UUID.fromString(message.uuid);
            switch (message.action) {
               case UPSERT -> {
                  RuneProgram requested = RuneProgram.fromNBT(message.program == null ? new CompoundTag() : message.program);
                  RuneProgram saved = RuneProgramService.upsert(player, id, requested);
                  if (saved == null) {
                     String reason = RuneProgramService.saveFailureReason(
                        player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES), requested);
                     player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.typemoonworld.rune.save_failed"), true);
                     TYPE_MOON_WORLD.LOGGER.warn("Rejected rune program save for {}: {}", player.getGameProfile().getName(), reason);
                  }
                  else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.typemoonworld.rune.saved", saved.displayName()), true);
               }
               case DELETE -> RuneProgramService.remove(player, id);
               case COPY -> RuneProgramService.copy(player, id);
               default -> { }
            }
         } catch (Exception exception) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.typemoonworld.rune.save_failed"), true);
            TYPE_MOON_WORLD.LOGGER.warn("Failed to update rune program for {} (action={}, uuid={})",
               player.getGameProfile().getName(), message.action, message.uuid, exception);
         }
      });
   }
}
