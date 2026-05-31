package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.FriendlyByteBuf;
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

public record OpenProjectionGuiMessage() implements CustomPacketPayload {
   public static final Type<OpenProjectionGuiMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "open_projection_gui"));
   public static final StreamCodec<FriendlyByteBuf, OpenProjectionGuiMessage> STREAM_CODEC = StreamCodec.unit(new OpenProjectionGuiMessage());

   @NotNull
   public Type<OpenProjectionGuiMessage> type() {
      return TYPE;
   }

   public static void handleData(OpenProjectionGuiMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.CLIENTBOUND) {
         context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
               OpenProjectionGuiMessage.ClientHandler.handle();
            }
         });
      }
   }

   private static class ClientHandler {
      public static void handle() {
         ClientPacketHandler.openProjectionGui();
      }
   }
}
