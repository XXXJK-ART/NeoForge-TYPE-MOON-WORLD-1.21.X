package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.client.TerrainDebrisClient;
import org.jetbrains.annotations.NotNull;

public record TerrainDebrisMessage(Vec3 center, long seed, List<Sample> samples) implements CustomPacketPayload {
   public static final Type<TerrainDebrisMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "terrain_debris"));
   public static final StreamCodec<FriendlyByteBuf, TerrainDebrisMessage> STREAM_CODEC = StreamCodec.of(TerrainDebrisMessage::write, TerrainDebrisMessage::read);
   private static final int MAX_SAMPLES = 48;

   public TerrainDebrisMessage {
      samples = List.copyOf(samples.subList(0, Math.min(MAX_SAMPLES, samples.size())));
   }

   @Override @NotNull public Type<TerrainDebrisMessage> type() { return TYPE; }

   private static void write(FriendlyByteBuf buffer, TerrainDebrisMessage message) {
      buffer.writeVec3(message.center);
      buffer.writeLong(message.seed);
      buffer.writeVarInt(message.samples.size());
      for (Sample sample : message.samples) {
         buffer.writeBlockPos(sample.pos);
         buffer.writeVarInt(sample.stateId);
      }
   }

   private static TerrainDebrisMessage read(FriendlyByteBuf buffer) {
      Vec3 center = buffer.readVec3();
      long seed = buffer.readLong();
      int count = Math.min(MAX_SAMPLES, buffer.readVarInt());
      List<Sample> samples = new ArrayList<>(count);
      for (int i = 0; i < count; i++) samples.add(new Sample(buffer.readBlockPos(), buffer.readVarInt()));
      return new TerrainDebrisMessage(center, seed, samples);
   }

   public static void handleData(TerrainDebrisMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.CLIENTBOUND) {
         context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) TerrainDebrisClient.spawn(message);
         });
      }
   }

   public record Sample(BlockPos pos, int stateId) { }
}
