package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MysticEyesItem;

public record MysticEyesToggleMessage(int eventType) implements CustomPacketPayload {
   public static final Type<MysticEyesToggleMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "mystic_eyes_toggle"));
   public static final StreamCodec<RegistryFriendlyByteBuf, MysticEyesToggleMessage> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT, MysticEyesToggleMessage::eventType, MysticEyesToggleMessage::new
   );

   public Type<MysticEyesToggleMessage> type() {
      return TYPE;
   }

   public static void handleData(MysticEyesToggleMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(() -> pressAction(context.player(), message.eventType)).exceptionally(e -> {
            TYPE_MOON_WORLD.LOGGER.error("Failed to handle MysticEyesToggleMessage", e);
            return null;
         });
      }
   }

   public static void pressAction(Player player, int type) {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.mysticEyesInventory.getSlots() > 0) {
         if (vars.mysticEyesInventory.getStackInSlot(0).getItem() instanceof MysticEyesItem) {
            vars.is_mystic_eyes_active = !vars.is_mystic_eyes_active;
            if (vars.is_mystic_eyes_active) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.mystic_eyes.activated"), true);
            } else {
               player.displayClientMessage(Component.translatable("message.typemoonworld.mystic_eyes.deactivated"), true);
            }

            vars.syncPlayerVariables(player);
         } else {
            if (vars.is_mystic_eyes_active) {
               vars.is_mystic_eyes_active = false;
               vars.syncPlayerVariables(player);
            }

            player.displayClientMessage(Component.translatable("message.typemoonworld.mystic_eyes.not_equipped"), true);
         }
      }
   }
}
