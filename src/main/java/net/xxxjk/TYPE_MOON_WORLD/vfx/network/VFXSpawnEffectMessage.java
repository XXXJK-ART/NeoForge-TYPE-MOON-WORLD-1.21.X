package net.xxxjk.TYPE_MOON_WORLD.vfx.network;

import java.util.Optional;
import java.util.UUID;
import java.util.Optional;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.vfx.client.VFXClientRuntime;
import org.jetbrains.annotations.NotNull;

public record VFXSpawnEffectMessage(String effectId, double x, double y, double z, Optional<UUID> targetEntityUuid, String dimension, long seed, Optional<Vec3> direction)
   implements CustomPacketPayload {
   public static final Type<VFXSpawnEffectMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "vfx_spawn_effect"));
   public static final StreamCodec<FriendlyByteBuf, VFXSpawnEffectMessage> STREAM_CODEC = StreamCodec.of(VFXSpawnEffectMessage::write, VFXSpawnEffectMessage::read);

   public VFXSpawnEffectMessage(String effectId, double x, double y, double z, Optional<UUID> targetEntityUuid, String dimension, long seed) {
      this(effectId, x, y, z, targetEntityUuid, dimension, seed, Optional.empty());
   }

   @Override
   @NotNull
   public Type<VFXSpawnEffectMessage> type() {
      return TYPE;
   }

   private static void write(FriendlyByteBuf buffer, VFXSpawnEffectMessage message) {
      buffer.writeUtf(message.effectId, 128);
      buffer.writeDouble(message.x);
      buffer.writeDouble(message.y);
      buffer.writeDouble(message.z);
      buffer.writeBoolean(message.targetEntityUuid.isPresent());
      message.targetEntityUuid.ifPresent(buffer::writeUUID);
      buffer.writeUtf(message.dimension, 128);
      buffer.writeLong(message.seed);
      buffer.writeBoolean(message.direction.isPresent());
      message.direction.ifPresent(v -> { buffer.writeDouble(v.x); buffer.writeDouble(v.y); buffer.writeDouble(v.z); });
   }

   private static VFXSpawnEffectMessage read(FriendlyByteBuf buffer) {
      String effectId = buffer.readUtf(128);
      double x = buffer.readDouble();
      double y = buffer.readDouble();
      double z = buffer.readDouble();
      Optional<UUID> target = buffer.readBoolean() ? Optional.of(buffer.readUUID()) : Optional.empty();
      String dimension = buffer.readUtf(128);
      long seed = buffer.readLong();
      Optional<Vec3> direction = buffer.readBoolean() ? Optional.of(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())) : Optional.empty();
      return new VFXSpawnEffectMessage(effectId, x, y, z, target, dimension, seed, direction);
   }

   public static void handleData(VFXSpawnEffectMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.CLIENTBOUND) {
         context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
               VFXClientRuntime.spawn(message.effectId, message.x, message.y, message.z, message.targetEntityUuid, message.seed, message.direction);
            }
         });
      }
   }
}
