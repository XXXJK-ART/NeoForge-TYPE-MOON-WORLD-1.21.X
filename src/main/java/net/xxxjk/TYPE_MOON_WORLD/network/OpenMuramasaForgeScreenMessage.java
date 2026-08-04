package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ClientPacketHandler;
import org.jetbrains.annotations.NotNull;

public record OpenMuramasaForgeScreenMessage() implements CustomPacketPayload {
   public static final Type<OpenMuramasaForgeScreenMessage> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "open_muramasa_forge_screen"));
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenMuramasaForgeScreenMessage> STREAM_CODEC =
      StreamCodec.unit(new OpenMuramasaForgeScreenMessage());

   @Override
   @NotNull
   public Type<OpenMuramasaForgeScreenMessage> type() {
      return TYPE;
   }

   public static void handleData(OpenMuramasaForgeScreenMessage message, IPayloadContext context) {
      context.enqueueWork(ClientPacketHandler::openMuramasaForgeScreen);
   }
}
