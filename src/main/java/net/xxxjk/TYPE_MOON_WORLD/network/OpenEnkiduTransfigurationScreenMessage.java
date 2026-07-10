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

public record OpenEnkiduTransfigurationScreenMessage() implements CustomPacketPayload {
   public static final Type<OpenEnkiduTransfigurationScreenMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "open_enkidu_transfiguration_screen")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenEnkiduTransfigurationScreenMessage> STREAM_CODEC = StreamCodec.unit(
      new OpenEnkiduTransfigurationScreenMessage()
   );

   @NotNull
   @Override
   public Type<OpenEnkiduTransfigurationScreenMessage> type() {
      return TYPE;
   }

   public static void handleData(OpenEnkiduTransfigurationScreenMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.CLIENTBOUND) {
         context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
               ClientPacketHandler.openEnkiduTransfigurationScreen();
            }
         });
      }
   }
}
