package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ClientPacketHandler;
import org.jetbrains.annotations.NotNull;

public record OpenMedeaCraftScreenMessage(int dragonfangStock, int manaCharmStock, int healCharmStock, int leylineMapStock) implements CustomPacketPayload {
   public static final Type<OpenMedeaCraftScreenMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "open_medea_craft_screen")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenMedeaCraftScreenMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeInt(message.dragonfangStock);
         buffer.writeInt(message.manaCharmStock);
         buffer.writeInt(message.healCharmStock);
         buffer.writeInt(message.leylineMapStock);
      },
      buffer -> new OpenMedeaCraftScreenMessage(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt())
   );

   @NotNull
   @Override
   public Type<OpenMedeaCraftScreenMessage> type() {
      return TYPE;
   }

   public static void handleData(OpenMedeaCraftScreenMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.CLIENTBOUND) {
         context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
               ClientPacketHandler.openMedeaCraftScreen(message.dragonfangStock, message.manaCharmStock, message.healCharmStock, message.leylineMapStock);
            }
         });
      }
   }
}
