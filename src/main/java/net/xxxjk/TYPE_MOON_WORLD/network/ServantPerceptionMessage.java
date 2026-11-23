package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ClientPacketHandler;
import org.jetbrains.annotations.NotNull;

public record ServantPerceptionMessage(List<Target> targets) implements CustomPacketPayload {
   private static final int MAX_TARGETS = 128;
   public static final Type<ServantPerceptionMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "servant_perception"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ServantPerceptionMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         List<Target> values = message.targets == null ? List.of() : message.targets;
         int size = Math.min(MAX_TARGETS, values.size());
         buffer.writeVarInt(size);
         for (int i = 0; i < size; i++) {
            Target target = values.get(i);
            buffer.writeVarInt(target.entityId());
            buffer.writeVarInt(Math.max(0, Math.min(3, target.color())));
         }
      },
      buffer -> {
         int size = Math.max(0, Math.min(MAX_TARGETS, buffer.readVarInt()));
         List<Target> values = new ArrayList<>(size);
         for (int i = 0; i < size; i++) values.add(new Target(buffer.readVarInt(), buffer.readVarInt()));
         return new ServantPerceptionMessage(values);
      });

   public record Target(int entityId, int color) { }
   @Override @NotNull public Type<ServantPerceptionMessage> type() { return TYPE; }
   public static void handleData(ServantPerceptionMessage message, IPayloadContext context) {
      context.enqueueWork(() -> ClientPacketHandler.handleServantPerception(message.targets));
   }
}
