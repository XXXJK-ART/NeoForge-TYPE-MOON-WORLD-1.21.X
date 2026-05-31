package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.procedures.Open_basic_information;
import org.jetbrains.annotations.NotNull;

public record Basic_information_gui_Message(int eventType, int pressedms) implements CustomPacketPayload {
   public static final Type<Basic_information_gui_Message> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "key_basicinformationgui")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, Basic_information_gui_Message> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
      buffer.writeInt(message.eventType);
      buffer.writeInt(message.pressedms);
   }, buffer -> new Basic_information_gui_Message(buffer.readInt(), buffer.readInt()));

   @NotNull
   public Type<Basic_information_gui_Message> type() {
      return TYPE;
   }

   public static void handleData(Basic_information_gui_Message message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(() -> pressAction(context.player(), message.eventType, message.pressedms)).exceptionally(e -> {
            TYPE_MOON_WORLD.LOGGER.error("Failed to handle Basic_information_gui_Message", e);
            return null;
         });
      }
   }

   public static void pressAction(Player entity, int type, int pressedms) {
      Level world = entity.level();
      double x = entity.getX();
      double y = entity.getY();
      double z = entity.getZ();
      if (world.isLoaded(entity.blockPosition()) && type == 0) {
         Open_basic_information.execute(world, x, y, z, entity);
      }
   }
}
