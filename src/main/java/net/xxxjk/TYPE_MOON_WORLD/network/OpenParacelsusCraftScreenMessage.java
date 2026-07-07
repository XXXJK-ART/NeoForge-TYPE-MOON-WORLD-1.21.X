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

public record OpenParacelsusCraftScreenMessage(int stoneStock, int diamondShieldStock) implements CustomPacketPayload {
   public static final Type<OpenParacelsusCraftScreenMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "open_paracelsus_craft_screen")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenParacelsusCraftScreenMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeInt(message.stoneStock);
         buffer.writeInt(message.diamondShieldStock);
      },
      buffer -> new OpenParacelsusCraftScreenMessage(buffer.readInt(), buffer.readInt())
   );

   @NotNull
   @Override
   public Type<OpenParacelsusCraftScreenMessage> type() {
      return TYPE;
   }

   public static void handleData(OpenParacelsusCraftScreenMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.CLIENTBOUND) {
         context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
               ClientPacketHandler.openParacelsusCraftScreen(message.stoneStock, message.diamondShieldStock);
            }
         });
      }
   }
}
