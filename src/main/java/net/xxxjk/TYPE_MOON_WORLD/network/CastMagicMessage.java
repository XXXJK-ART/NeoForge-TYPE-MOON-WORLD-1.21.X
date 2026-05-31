package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.nordic.MagicGander;
import net.xxxjk.TYPE_MOON_WORLD.procedures.CastMagic;

public record CastMagicMessage(int eventType, int pressedms) implements CustomPacketPayload {
   public static final Type<CastMagicMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "cast_magic"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CastMagicMessage> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
      buffer.writeInt(message.eventType);
      buffer.writeInt(message.pressedms);
   }, buffer -> new CastMagicMessage(buffer.readInt(), buffer.readInt()));

   public CastMagicMessage() {
      this(0, 0);
   }

   public static void pressAction(Player entity) {
      CastMagic.execute(entity);
   }

   public static void pressAction(Player entity, int eventType, int pressedMs) {
      if (entity instanceof ServerPlayer serverPlayer) {
         handleEvent(serverPlayer, eventType);
      }
   }

   public Type<CastMagicMessage> type() {
      return TYPE;
   }

   public static void handleData(CastMagicMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
               handleEvent(serverPlayer, message.eventType);
            }
         }).exceptionally(e -> {
            TYPE_MOON_WORLD.LOGGER.error("Failed to handle CastMagicMessage", e);
            return null;
         });
      }
   }

   private static void handleEvent(ServerPlayer player, int eventType) {
      switch (eventType) {
         case 0:
            CastMagic.execute(player);
            break;
         case 1:
            MagicGander.beginCharge(player);
            break;
         case 2:
            boolean released = MagicGander.releaseCharge(player);
            if (released) {
               TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                  TypeMoonWorldModVariables.PLAYER_VARIABLES
               );
               vars.magic_cooldown = 10.0;
               vars.syncMana(player);
            }
      }
   }
}
