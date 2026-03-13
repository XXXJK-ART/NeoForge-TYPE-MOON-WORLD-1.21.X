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

public record OpenLeylineSurveyMapMessage(int gridSize, int centerChunkX, int centerChunkZ, String dimensionId, byte[] concentrations)
   implements CustomPacketPayload {
   private static final int MAX_GRID_SIZE = 100;
   private static final int MAX_DIMENSION_ID_LENGTH = 128;
   private static final int MAX_DATA_LENGTH = 10000;
   public static final Type<OpenLeylineSurveyMapMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "open_leyline_survey_map"));
   public static final StreamCodec<FriendlyByteBuf, OpenLeylineSurveyMapMessage> STREAM_CODEC = StreamCodec.of(
      OpenLeylineSurveyMapMessage::write, OpenLeylineSurveyMapMessage::read
   );

   @NotNull
   public Type<OpenLeylineSurveyMapMessage> type() {
      return TYPE;
   }

   private static void write(FriendlyByteBuf buffer, OpenLeylineSurveyMapMessage message) {
      buffer.writeVarInt(message.gridSize);
      buffer.writeInt(message.centerChunkX);
      buffer.writeInt(message.centerChunkZ);
      buffer.writeUtf(message.dimensionId, 128);
      buffer.writeByteArray(message.concentrations);
   }

   private static OpenLeylineSurveyMapMessage read(FriendlyByteBuf buffer) {
      int gridSize = buffer.readVarInt();
      int centerChunkX = buffer.readInt();
      int centerChunkZ = buffer.readInt();
      String dimensionId = buffer.readUtf(128);
      byte[] concentrations = buffer.readByteArray(10000);
      return new OpenLeylineSurveyMapMessage(gridSize, centerChunkX, centerChunkZ, dimensionId, concentrations);
   }

   public static void handleData(OpenLeylineSurveyMapMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.CLIENTBOUND) {
         context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
               OpenLeylineSurveyMapMessage.ClientHandler.handle(message);
            }
         });
      }
   }

   private static class ClientHandler {
      public static void handle(OpenLeylineSurveyMapMessage message) {
         ClientPacketHandler.openLeylineSurveyMap(
            message.gridSize(), message.centerChunkX(), message.centerChunkZ(), message.dimensionId(), message.concentrations()
         );
      }
   }
}
