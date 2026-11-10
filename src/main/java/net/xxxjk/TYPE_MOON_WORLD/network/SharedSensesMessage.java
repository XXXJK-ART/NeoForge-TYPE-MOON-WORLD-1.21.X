package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ClientPacketHandler;
import org.jetbrains.annotations.NotNull;

public record SharedSensesMessage(boolean active, int entityId) implements CustomPacketPayload {
   public static final Type<SharedSensesMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "shared_senses"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SharedSensesMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> { buffer.writeBoolean(message.active()); buffer.writeVarInt(message.entityId()); },
      buffer -> new SharedSensesMessage(buffer.readBoolean(), buffer.readVarInt()));
   @Override @NotNull public Type<SharedSensesMessage> type() { return TYPE; }
   public static void handleData(SharedSensesMessage message, IPayloadContext context) {
      context.enqueueWork(() -> ClientPacketHandler.handleSharedSenses(message.active(), message.entityId()));
   }
}
