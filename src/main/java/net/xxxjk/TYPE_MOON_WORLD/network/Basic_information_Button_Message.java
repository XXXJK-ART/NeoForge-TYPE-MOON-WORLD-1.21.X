package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.core.BlockPos;
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
import net.xxxjk.TYPE_MOON_WORLD.procedures.To_magical_attributes;
import org.jetbrains.annotations.NotNull;

public record Basic_information_Button_Message(int buttonID, int x, int y, int z) implements CustomPacketPayload {
   public static final Type<Basic_information_Button_Message> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "basicinformation_buttons")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, Basic_information_Button_Message> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
      buffer.writeInt(message.buttonID);
      buffer.writeInt(message.x);
      buffer.writeInt(message.y);
      buffer.writeInt(message.z);
   }, buffer -> new Basic_information_Button_Message(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()));

   @NotNull
   public Type<Basic_information_Button_Message> type() {
      return TYPE;
   }

   public static void handleData(Basic_information_Button_Message message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(() -> {
            Player entity = context.player();
            int buttonID = message.buttonID;
            int x = message.x;
            int y = message.y;
            int z = message.z;
            handleButtonAction(entity, buttonID, x, y, z);
         }).exceptionally(e -> {
            TYPE_MOON_WORLD.LOGGER.error("Failed to handle Basic_information_Button_Message", e);
            return null;
         });
      }
   }

   public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
      Level world = entity.getCommandSenderWorld();
      BlockPos pos = BlockPos.containing(x, y, z);
      if (world.isLoaded(pos)) {
         if (!world.isClientSide() && (buttonID == 0 || buttonID == 1)) {
            To_magical_attributes.execute(world, x, y, z, entity, buttonID);
         }
      }
   }
}
